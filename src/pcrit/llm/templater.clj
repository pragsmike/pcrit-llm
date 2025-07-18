(ns pcrit.llm.templater
  (:require [selmer.parser :as selmer]
            [pcrit.llm.core :as core]))

(defn expand
  "Renders a template string with a map of variables."
  [template vars-map]
  (selmer/render template vars-map))

(defn call-model-template
  "Renders a template with variables and calls an LLM with the result.
  Accepts an optional fourth argument, `llm-fn`, for testing."
  ([model-name prompt-template-string vars-map]
   (call-model-template model-name prompt-template-string vars-map core/call-model))
  ([model-name prompt-template-string vars-map llm-fn]
   (llm-fn model-name (expand prompt-template-string vars-map))))
