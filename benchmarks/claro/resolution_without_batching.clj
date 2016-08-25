(ns claro.resolution-without-batching
  (:require [perforate.core :refer [defgoal defcase]]))

;; ## Testcase
;;
;; We'll model a three-level resolution for all candidates:
;; - resolve Person with :image-id and :friend-ids,
;; - resolve :friend-ids as a seq of [Person]
;; - resolve :image-id into an Image.

(defgoal resolution-without-batching
  "Resolution of a finite tree of Resolvables (without batching).")
