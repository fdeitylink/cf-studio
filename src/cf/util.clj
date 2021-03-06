(ns cf.util
  (:require [clojure.java.io :as io]
            [clojure.spec.alpha :as spec]
            loom.graph
            [org.clojars.smee.binary.core :as bin]))

(spec/def ::byte (spec/int-in -0x80 0x80))
(spec/def ::ubyte (spec/int-in 0x0 0x100))

(spec/def ::short (spec/int-in -0x8000 0x8000))
(spec/def ::ushort (spec/int-in 0x0 0x10000))

(defn validator
  "Takes a spec and returns a function validating its argument against that spec.
  If the value passes the spec, the function returns it. Otherwise, it throws an ExceptionInfo object with an optional error message and the spec explanation."
  ([spec]
   (validator spec ""))
  ([spec err-str]
   #(if (spec/valid? spec %)
      %
      (throw (ex-info err-str (spec/explain-data spec %))))))

(defn decode-file
  "Decodes a file according to a codec."
  [codec path]
  (with-open [is (io/input-stream path)]
    (bin/decode codec is)))

(defn encode-file
  "Encodes a value into a file according to a codec."
  [codec path value]
  (with-open [os (io/output-stream path)]
    (bin/encode codec os value)))

(def running-in-repl?
  "Represents whether or not the Clojure runtime is being executed from a REPL."
  false)

(defn set-running-in-repl!
  "Sets `running-in-repl?`.
  Should be executed on the main thread."
  []
  (alter-var-root
   #'running-in-repl?
   (fn [_]
     (boolean
      (some
       #(and (= "clojure.main$repl" (.getClassName ^StackTraceElement %))
             (= "doInvoke" (.getMethodName ^StackTraceElement %)))
       (.getStackTrace (Thread/currentThread)))))))

(defn disj*
  "Same as [[disj]] but takes keyseq instead of rest arg."
  [set ks]
  (apply disj set ks))

(defn filter-nodes
  "Same as [[loom.derived/nodes-filtered-by]] but retains attributes."
  [pred g]
  (loom.graph/remove-nodes* g (remove pred (loom.graph/nodes g))))

(defn find-first
  "Returns the first item in `coll` satisfying `pred`, else nil."
  [pred coll]
  (reduce
   (fn [_ x]
     (when (pred x)
       (reduced x)))
   nil
   coll))
