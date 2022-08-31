(ns com.github.ivarref.locksmith
  (:require [clojure.java.io :as io])
  (:import (java.util.concurrent TimeUnit)
           (okhttp3.tls HeldCertificate HeldCertificate$Builder)))

(defn gen-certs [{:keys [duration-days]
                  :or   {duration-days 365}}]
  (let [^HeldCertificate rootCertificate (-> (HeldCertificate$Builder.)
                                             (.certificateAuthority 0)
                                             (.duration duration-days TimeUnit/DAYS)
                                             (.build))
        ^HeldCertificate serverCertificate (-> (HeldCertificate$Builder.)
                                               (.signedBy rootCertificate)
                                               (.duration duration-days TimeUnit/DAYS)
                                               (.build))
        ^HeldCertificate client (-> (HeldCertificate$Builder.)
                                    (.signedBy rootCertificate)
                                    (.duration duration-days TimeUnit/DAYS)
                                    (.build))]
    {:ca-cert     (.certificatePem rootCertificate)
     :ca-key      (.privateKeyPkcs8Pem rootCertificate)
     :server-cert (.certificatePem serverCertificate)
     :server-key  (.privateKeyPkcs8Pem serverCertificate)
     :client-cert (.certificatePem client)
     :client-key  (.privateKeyPkcs8Pem client)}))

(defn set-permissions! [f]
  (when f
    (let [f (io/file f)]
      (.setExecutable f false)
      (.setReadable f false false)
      (.setReadable f true true)
      (.setWritable f false))))

(defn write-certs! [{:keys [server-out-file client-out-file]
                     :or {server-out-file "server.keys"
                          client-out-file "client.keys"}
                     :as opts}]
  (let [{:keys [ca-cert
                server-cert server-key
                client-cert client-key]}
        (gen-certs opts)]
    (when (.exists (io/file server-out-file))
      (.delete (io/file server-out-file)))
    (spit server-out-file (str ca-cert server-cert server-key))
    (set-permissions! server-out-file)
    (println "Wrote" server-out-file)

    (when (.exists (io/file client-out-file))
      (.delete (io/file client-out-file)))
    (spit client-out-file (str ca-cert client-cert client-key))
    (set-permissions! client-out-file)
    (println "Wrote" client-out-file)))
