(ns todomvc.views
  (:require [reagent.core  :as reagent]
            [re-frame.core :refer [subscribe dispatch]]
            [clojure.string :as str])
  (:import
        (goog.i18n NumberFormat)
        (goog.i18n.NumberFormat Format)))


(defn todo-input [{:keys [title on-save on-stop]}]
  (let [val  (reagent/atom title)
        stop #(do (reset! val "")
                  (when on-stop (on-stop)))
        save #(let [v (-> @val str str/trim)]
                (when (seq v) (on-save v))
                (stop))]
    (fn [props]
      [:input (merge (dissoc props :on-save :on-stop :title)
                     {:type        "text"
                      :value       @val
                      :auto-focus  true
                      :on-blur     save
                      :on-change   #(reset! val (-> % .-target .-value))
                      :on-key-down #(case (.-which %)
                                      13 (save)
                                      27 (stop)
                                      nil)})])))


(defn todo-item
  []
  (let [editing (reagent/atom false)]
    (fn [{:keys [id done title]}]
      [:li {:class (str (when done "completed ")
                        (when @editing "editing"))}
        [:div.view
          [:input.toggle
            {:type "checkbox"
             :checked done
             :on-change #(dispatch [:toggle-done id])}]
          [:label
            {:on-double-click #(reset! editing true)}
            title]
          [:button.destroy
            {:on-click #(dispatch [:delete-todo id])}]]
        (when @editing
          [todo-input
            {:class "edit"
             :title title
             :on-save #(dispatch [:save id %])
             :on-stop #(reset! editing false)}])])))


(defn task-list
  []
  (let [visible-todos @(subscribe [:visible-todos])
        all-complete? @(subscribe [:all-complete?])]
      [:section#main
        [:input#toggle-all
          {:type "checkbox"
           :checked all-complete?
           :on-change #(dispatch [:complete-all-toggle])}]
        [:label
          {:for "toggle-all"}
          "Mark all as complete"]
        [:ul#todo-list
          (for [todo  visible-todos]
            ^{:key (:id todo)} [todo-item todo])]]))


(defn footer-controls
  []
  (let [[active done] @(subscribe [:footer-counts])
        showing       @(subscribe [:showing])
        a-fn          (fn [filter-kw txt]
                        [:a {:class (when (= filter-kw showing) "selected")
                             :href (str "#/" (name filter-kw))} txt])]
    [:footer#footer
     [:span#todo-count
      [:strong active] " " (case active 1 "item" "items") " left"]
     [:ul#filters
      [:li (a-fn :all    "All")]
      [:li (a-fn :active "Active")]
      [:li (a-fn :done   "Completed")]]
     (when (pos? done)
       [:button#clear-completed {:on-click #(dispatch [:clear-completed])}
        "Clear completed"])]))


(defn task-entry
  []
  [:header#header
    [:h1 "todos"]
    [todo-input
      {:id "new-todo"
       :placeholder "What needs to be done?"
       :on-save #(dispatch [:add-todo %])}]])

(defn capture-input [{:keys [initial-value on-change]}]
  (let [text-value (reagent/atom initial-value)]
    (fn []
      [:div.dollar-input
       [:span.dollar-sign "$"]
       [:input {:value     @text-value
                :type      "text"
                :on-change (fn [e]
                             (reset! text-value (.-target.value e)))
                :on-blur   (fn [e]
                             (on-change (.-target.value e)))}]])))

(defn my-example-2 []
  (let [my-data @(subscribe [:my-example-2-data])]
    [:div
     [:h1 "My Example 2"]
     [:p (with-out-str (cljs.pprint/pprint my-data))]
     [:input {:value     (:other-value my-data)
              :on-change #(dispatch [:set-my-example-2-data
                                     (assoc my-data :other-value (.-target.value %))])}]
     [capture-input {:initial-value (:amount my-data)
                    :on-change      (fn [v]
                                     (dispatch [:set-my-example-2-data
                                                (assoc my-data :amount v)]))}]]))

(defn todo-app
  []
  [:div
   [my-example-2]
   [:section#todoapp
    [task-entry]
    (when (seq @(subscribe [:todos]))
      [task-list])
    [footer-controls]]
   [:footer#info
    [:p "Double-click to edit a todo"]]])
