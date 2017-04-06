#!/usr/bin/env boot

(set-env! :dependencies '[[org.clojars.scsibug/feedparser-clj "0.4.0"]
                          [org.clojure/data.xml "0.0.8"]
                          [me.raynes/conch "0.8.0"]
                          [me.raynes/fs "1.4.6"]])

(require '[feedparser-clj.core :refer [parse-feed]]
         '[boot.cli :refer [defclifn]]
         '[clojure.data.xml :as Xml]
         '[clojure.java.shell :as sh]
         '[clojure.string :as Str]
         '[clojure.java.io :as io]
         '[me.raynes.conch :as ch]
         '[me.raynes.fs :as fs])

(defn feed? [element]
  (-> element :attrs :xmlUrl))

(defn http->https [url]
  (Str/replace url #"http://" "https://"))

(defn fetch-entries [xmlUrl]
  (println "TEST")
  (try
    (-> xmlUrl parse-feed :entries)
   (catch Exception e ;; try https
     (try
       (-> xmlUrl http->https parse-feed :entries)
     (catch Exception e
       (println "error" xmlUrl)
       nil))))) ;; give up

(defn analyze-feed [{{:keys [title xmlUrl]} :attrs}]
  (if-let [entries (fetch-entries xmlUrl)]
    (let []
      (println "ok" title)
      )
    [title nil]))

(defn analyze-opml [opml]
  (->> (Xml/parse-str opml)
        (xml-seq)
        (filter feed?)
        rest
        (map analyze-feed)))

(defn format-analysis [analysis]
  analysis)

(defclifn
  -main "A simple clojure script."
  [o out FILE file "output file."]
  (let [in (when-let [in (first *args*)] (io/as-file in))]
    (with-open [r (io/reader (or in *in*))]
      (with-open [w (io/writer (or out *out*))]
        (->> (slurp r)
             analyze-opml
             format-analysis
             (spit w))))))
