(ns spootnik.deps-proto
  (:require [clojure.tools.build.api :as b]
            [babashka.fs :as fs]))

(def default-source-path
  "proto")

(def default-classes-path
  (str (fs/path "target" "classes")))

(def default-generate-path
  (str (fs/path "target" "java")))

(defn compile-java
  [{:keys [generate-path classes-path]
    :or   {generate-path default-generate-path
           classes-path  default-classes-path}
    :as   opts}]
  (b/javac {:src-dirs  [generate-path]
            :class-dir classes-path
            :basis     (b/create-basis {:project "deps.edn"})})
  opts)

(defn generate
  [{:keys [source-path generate-path proto-files protoc-bin]
    :or   {source-path   default-source-path
           generate-path default-generate-path
           protoc-bin    "protoc"}
    :as   opts}]
  (fs/create-dirs source-path)
  (fs/create-dirs generate-path)
  (let [proto-files (or proto-files (for [f (fs/glob source-path "**.proto")]
                                      (str (fs/relativize source-path f))))
        cmdline     (concat [protoc-bin
                             (str "-I" (or source-path default-source-path))
                             (str "--java_out=" (or generate-path "target/java"))]
                            proto-files)
        res         (b/process {:command-args cmdline})]
    (when-not (zero? (:exit res))
      (binding [*out* *err*]
        (println "failed to generate protobuf code")
        (throw (ex-info "failed to generate protobuf code" res))))))

(def generate-and-compile
  (comp compile-java generate))
