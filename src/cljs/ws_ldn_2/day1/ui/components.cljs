(ns ws-ldn-2.day1.ui.components)

(defn dropdown
  "Dropdown component. Takes currently selected value, on-change handler
  and a map of menu items, where keys are used as the <option> items' values.
  The map's values are expected to be maps themselves and need to have at
  least a :label key. If the :label is missing the item's key is used as label."
  [sel on-change opts]
  [:select
   {:defaultValue sel
    :on-change    on-change}
   (map
    (fn [[id val]]
      [:option {:key (str "dd" id) :value (name id)} (or (:label val) (name id))])
    opts)])
