(ns frontend.state)

(defn initial-state []
  {:camera          {:x          0
                     :y          0
                     :zf         1
                     :z-exact    1
                     :offset-x   0
                     :offset-y   0
                     :show-grid? true}
   :error-message   nil
   :changelog       nil
   :environment     "development"
   :settings        {:browser-settings {:current-tool :pen
                                        :aside-menu-opened false
                                        :chat-mobile-opened true
                                        :right-click-learned false
                                        :menu-button-learned false
                                        :info-button-learned false
                                        :newdoc-button-learned false
                                        :main-menu-learned false
                                        :login-button-learned false}}
   :keyboard-shortcuts {:select #{"s"}
                        :circle #{"c"}
                        :rect #{"r"}
                        :line #{"l"}
                        :pen #{"p"}
                        :text #{"t"}
                        :undo #{"meta+z" "ctrl+z"}
                        :shortcuts-menu #{"shift+/"}
                        :escape-interaction #{"esc"}
                        :reset-canvas-position #{"home" "1"}}
   :current-user    nil
   :instrumentation []
   :entity-ids      #{}
   :document/id     17592186046465
   :subscribers     {}
   ;; This isn't passed to the components, it can be accessed though om/get-shared :_app-state-do-not-use
   :aside-width     0
   :inputs          nil})

(def user-path [:current-user])

(def settings-path [:settings])

(def instrumentation-path [:instrumentation])

(def browser-settings-path [:settings :browser-settings])

(def account-subpage-path [:account-settings-subpage])
(def new-user-token-path (conj user-path :new-user-token))

(def flash-path [:render-context :flash])

(def error-data-path [:error-data])

(def selected-home-technology-tab-path [:selected-home-technology-tab])

(def language-testimonial-tab-path [:selected-language-testimonial-tab])

(def changelog-path [:changelog])

(def build-state-path [:build-state])

(def error-message-path [:error-message])

(def inputs-path [:inputs])

(def docs-data-path [:docs-data])
(def docs-search-path [:docs-query])
(def docs-articles-results-path [:docs-articles-results])
(def docs-articles-results-query-path [:docs-articles-results-query])

(def user-options-shown-path [:user-options-shown])

(def current-tool-path (conj browser-settings-path :current-tool))

(def aside-menu-opened-path (conj browser-settings-path :aside-menu-opened))

(def chat-mobile-opened-path (conj browser-settings-path :chat-mobile-toggled))

(def right-click-learned-path (conj browser-settings-path :right-click-learned))

(def menu-button-learned-path (conj browser-settings-path :menu-button-learned))

(def info-button-learned-path (conj browser-settings-path :info-button-learned))

(def newdoc-button-learned-path (conj browser-settings-path :newdoc-button-learned))

(def login-button-learned-path (conj browser-settings-path :login-button-learned))

(def your-docs-learned-path (conj browser-settings-path :your-docs-learned))

(def main-menu-learned-path (conj browser-settings-path :main-menu-learned))

(def invite-menu-learned-path (conj browser-settings-path :invite-menu-learned))

(def shortcuts-menu-learned-path (conj browser-settings-path :shortcuts-menu-learned))

(defn doc-settings-path [doc-id]
  (conj browser-settings-path :document-settings doc-id))

(defn last-read-chat-time-path [doc-id]
  (conj (doc-settings-path doc-id) :last-read-chat-time))

(defn doc-chat-bot-path [doc-id]
  (conj (doc-settings-path doc-id) :chat-bot))

(def keyboard-shortcuts-path [:keyboard-shortcuts])

(def overlay-info-opened-path [:aside-menu-opened])

(def overlay-username-opened-path [:overlay-username-opened])

(def overlay-shortcuts-opened-path [:overlay-shortcuts-opened])

(def aside-width-path [:aside-width])

(def overlays-path [:overlays])
