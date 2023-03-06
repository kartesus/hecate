(ns hecate.http.core
  (:require [ring.adapter.jetty :as jetty]
            [reitit.ring :as r]
            [muuntaja.middleware :refer [wrap-format]]
            [ring.util.http-response :refer [ok not-found created conflict]]))

(def notes (atom {"1" {:id "1" :title "First note" :content "This is the first note"}
                  "2" {:id "2" :title "Second note" :content "This is the second note"}}))

(defn get-all [req]
  (ok {:ok (vals @notes)}))

(defn get-one [req]
  (let [id (:id (:path-params req))
        note (get @notes id)]
    (if note
      (ok {:ok note})
      (not-found {:error (str "Note " id " not found")}))))

(defn create [req]
  (let [note (:body-params req)
        note-exists? (get @notes (:id note))]
    (if note-exists?
      (conflict {:error (str "Note " (:id note) " already exists")})
      (do
        (swap! notes assoc (:id note) note)
        (created (:id note))))))

(defn update [req]
  (let [id (:id (:path-params req))
        note (:body-params req)
        note-exists? (get @notes id)]
    (if note-exists?
      (do
        (swap! notes assoc id note)
        (ok {:ok note}))
      (not-found {:error (str "Note " id " not found")}))))

(defn delete [req]
  (let [id (:id (:path-params req))
        note-exists? (get @notes id)]
    (if note-exists?
      (do
        (swap! notes dissoc id)
        (ok {:ok (str "Note " id " deleted")}))
      (not-found {:error (str "Note " id " not found")}))))

(def app
  (r/ring-handler
   (r/router
    [["/notes/:id" {:get get-one :put update :delete delete}]
     ["/notes" {:get get-all :post create}]]
    {:data {:middleware [wrap-format]}})))

(defn run [& args]
  (jetty/run-jetty #'app {:port 8080, :join? false}))

(def server (atom nil))

(defn start-server []
  (reset! server (run)))

(defn stop-server []
  (.stop @server))

(comment
  (start-server)
  (stop-server))
