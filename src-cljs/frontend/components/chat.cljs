(ns frontend.components.chat
  (:require [clojure.set :as set]
            [clojure.string :as str]
            [cljs-time.core :as time]
            [cljs-time.format :as time-format]
            [datascript :as d]
            [frontend.async :refer [put!]]
            [frontend.components.common :as common]
            [frontend.datascript :as ds]
            [frontend.datetime :as datetime]
            [frontend.models.chat :as chat-model]
            [frontend.state :as state]
            [frontend.utils :as utils :include-macros true]
            [goog.date]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true])
  (:require-macros [sablono.core :refer (html)])
  (:import [goog.ui IdGenerator]))

(def url-regex #"(?im)\b(?:https?|ftp)://[-A-Za-z0-9+@#/%?=~_|!:,.;]*[-A-Za-z0-9+@#/%=~_|]")

(defn linkify [text]
  (let [matches (re-seq url-regex text)
        ;; may need to add [""], split can return empty array
        parts (or (seq (str/split text url-regex)) [""])]
    (reduce (fn [acc [pre url]]
              (conj acc [:span pre] (when url [:a {:href url :target "_blank"} url])))
            [:span] (partition-all 2 (concat (interleave parts
                                                         matches)
                                             (when (not= (count parts)
                                                         (count matches))
                                               [(last parts)]))))))


(defn chat-item [chat owner {:keys [sente-id show-sender?]}]
  (reify
    om/IDisplayName (display-name [_] "Chat Item")
    om/IRender
    (render [_]
      (let [id (apply str (take 6 (str (:session/uuid chat))))
            name (chat-model/display-name chat sente-id)
            chat-body (if (string? (:chat/body chat))
                        (linkify (:chat/body chat))
                        (:chat/body chat))
            short-time (datetime/short-time (js/Date.parse (:server/timestamp chat)))]
        (html [:div.chat-message {:key (str "chat-message" (:db/id chat))}
               (when show-sender?
                 [:div.message-head
                  [:span
                   (common/icon :user {:path-props {:style {:stroke (or (:chat/color chat) (str "#" id))}}})]
                  [:span (str " " name)]
                  [:span.time (str " " short-time)]])
               [:div.message-body
                chat-body]])))))

(def day-of-week
  {1 "Monday"
   2 "Tuesday"
   3 "Wednesday"
   4 "Thursday"
   5 "Friday"
   6 "Saturday"
   7 "Sunday"})

(def month-of-year
  {0 "Jan"
   1 "Feb"
   2 "Mar"
   3 "Apr"
   4 "May"
   5 "June"
   6 "July"
   7 "Aug"
   8 "Sep"
   9 "Oct"
   10 "Nov"
   11 "Dec"})

(defn date->bucket [date]
  (let [time (goog.date.DateTime. date)
        start-of-day (doto (goog.date.DateTime.)
                       (.setHours 0)
                       (.setMinutes 0)
                       (.setSeconds 0)
                       (.setMilliseconds 0))]
    (cond
     (time/after? time start-of-day) "Today"
     (time/after? time (time/minus start-of-day (time/days 1))) "Yesterday"
     (time/after? time (time/minus start-of-day (time/days 6))) (day-of-week (time/day-of-week time))
     :else (str (month-of-year (.getMonth time)) " " (.getDate time)))))

(defn input [{:keys [chat-body]} owner]
  (reify
    om/IDisplayName (display-name [_] "Chat Input")
    om/IInitState (init-state [_] {:chat-body (atom "")})
    om/IRender
    (render [_]
      (let [{:keys [cast!]} (om/get-shared owner)
            submit-chat (fn [e]
                          (cast! :chat-submitted {:chat-body @(om/get-state owner :chat-body)})
                          (reset! (om/get-state owner :chat-body) "")
                          (om/refresh! owner)
                          (utils/stop-event e))]
        (html
          [:div.chat-box
           [:form {:class "chat-form"
                   :on-submit submit-chat
                   :on-key-down #(when (and (= "Enter" (.-key %))
                                            (not (.-shiftKey %))
                                            (not (.-ctrlKey %))
                                            (not (.-metaKey %))
                                            (not (.-altKey %)))

                                   (submit-chat %))}
            [:textarea {:class "chat-input"
                        :id "chat-input"
                        :tab-index "1"
                        :type "text"
                        :value @(om/get-state owner :chat-body)
                        :placeholder "Chat..."
                        :on-change #(reset! (om/get-state owner :chat-body)
                                            (.. % -target -value))}]]])))))

(defn log [{:keys [db chat-body sente-id client-id chat-opened]} owner]
  (reify
    om/IDisplayName (display-name [_] "Chat Log")
    om/IInitState
    (init-state [_]
      {:listener-key (.getNextUniqueId (.getInstance IdGenerator))
       :auto-scroll? true
       :touch-enabled? false})
    om/IDidMount
    (did-mount [_]
      (om/set-state! owner :touch-enabled? (.hasOwnProperty js/window "ontouchstart"))
      (d/listen! (om/get-shared owner :db)
                 (om/get-state owner :listener-key)
                 (fn [tx-report]
                   ;; TODO: better way to check if state changed
                   (when-let [chat-datoms (seq (filter #(or (= :chat/body (:a %))
                                                            (= :document/chat-bot (:a %)))
                                                       (:tx-data tx-report)))]
                     (om/refresh! owner)))))
    om/IWillUnmount
    (will-unmount [_]
      (d/unlisten! (om/get-shared owner :db) (om/get-state owner :listener-key)))
    om/IWillReceiveProps
    (will-receive-props [_ next-props]
      ;; check for scrolled all of the way down
      (let [node (om/get-node owner "chat-messages")
            auto-scroll? (= (- (.-scrollHeight node) (.-scrollTop node))
                            (.-clientHeight node))]
        (when (not= (om/get-state owner :auto-scroll?) auto-scroll?)
          (om/set-state! owner :auto-scroll? auto-scroll?))))
    om/IDidUpdate
    (did-update [_ _ _]
      (when (om/get-state owner :auto-scroll?)
        (set! (.-scrollTop (om/get-node owner "chat-messages"))
              10000000)))
    om/IRender
    (render [_]
      (let [{:keys [cast!]} (om/get-shared owner)
            chats (ds/touch-all '[:find ?t :where [?t :chat/body]] @db)
            chat-bot (:document/chat-bot (d/entity @db (ffirst (d/q '[:find ?t :where [?t :document/name]] @db))))
            dummy-chat {:chat/body [:span
                                    "Welcome to Precursor! "
                                    "Create fast prototypes and share your url to collaborate. "
                                    "Chat "
                                    [:a {:on-click #(cast! :chat-user-clicked {:id-str (:chat-bot/name chat-bot)})
                                         :role "button"}
                                     (str "@" (:chat-bot/name chat-bot))]
                                    " for help."]
                        :chat/color "#00b233"
                        :session/uuid (:chat-bot/name chat-bot)
                        :server/timestamp (js/Date.)}]
        (html
         [:div.chat-log {:ref "chat-messages"}
          (when chat-bot
            (om/build chat-item dummy-chat {:opts {:show-sender? true}}))
          (let [chat-groups (group-by #(date->bucket (:server/timestamp %)) chats)]
            (for [[time chat-group] (sort-by #(:server/timestamp (first (second %)))
                                             chat-groups)]

              (list (when (or (not= 1 (count chat-groups))
                              (not= #{"Today"} (set (keys chat-groups))))
                      [:h2.chat-date time])
                    (for [[prev-chat chat] (partition 2 1 (concat [nil] (sort-by :server/timestamp chat-group)))]
                      (om/build chat-item chat
                                {:key :db/id
                                 :opts {:sente-id sente-id
                                        :show-sender? (not= (chat-model/display-name prev-chat sente-id)
                                                            (chat-model/display-name chat sente-id))}})))))])))))

(defn chat [app owner]
  (reify
    om/IDisplayName (display-name [_] "Chat")
    om/IRender
    (render [_]
      (let [{:keys [cast!]} (om/get-shared owner)
            controls-ch (om/get-shared owner [:comms :controls])
            client-id (:client-id app)
            chat-opened? (get-in app state/chat-opened-path)
            chat-mobile-open? (get-in app state/chat-mobile-opened-path)
            document-id (get-in app [:document/id])]
        (html
          [:div.chat {:class (when chat-opened? ["opened"])}
           [:div#canvas-size.chat-offset]
           [:div.chat-window {:class (when-not chat-opened? ["closed"])}
            [:div.chat-background]
            (om/build log {:db (:db app)
                           :document/id (:document/id app)
                           :sente-id (:sente-id app)
                           :client-id (:client-id app)
                           :chat-body (get-in app [:chat :body])
                           :chat-opened (get-in app state/chat-opened-path)})
            (om/build input {:chat-body (get-in app [:chat :body])})]])))))
