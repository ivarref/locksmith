(ns com.github.ivarref.locksmith
  (:require [clojure.java.io :as io]
            [clojure.string :as str])
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

(def generate-certs gen-certs)

(defn server-keys [the-keys]
  (assert (map? the-keys))
  (assert (contains? the-keys :ca-cert))
  (assert (contains? the-keys :server-cert))
  (assert (contains? the-keys :server-key))
  (assert (str/includes? (get the-keys :ca-cert) "-----BEGIN CERTIFICATE-----"))
  (assert (str/includes? (get the-keys :ca-cert) "-----END CERTIFICATE-----"))
  (assert (str/includes? (get the-keys :server-cert) "-----BEGIN CERTIFICATE-----"))
  (assert (str/includes? (get the-keys :server-cert) "-----END CERTIFICATE-----"))
  (assert (str/includes? (get the-keys :server-key) "-----BEGIN PRIVATE KEY-----"))
  (assert (str/includes? (get the-keys :server-key) "-----END PRIVATE KEY-----"))
  (str (get the-keys :ca-cert)
       (get the-keys :server-cert)
       (get the-keys :server-key)))

(defn client-keys [the-keys]
  (assert (map? the-keys) "Argument `the-keys` must be the result from calling `gen-certs`")
  (assert (contains? the-keys :ca-cert))
  (assert (contains? the-keys :client-cert))
  (assert (contains? the-keys :client-key))
  (assert (str/includes? (get the-keys :ca-cert) "-----BEGIN CERTIFICATE-----"))
  (assert (str/includes? (get the-keys :ca-cert) "-----END CERTIFICATE-----"))
  (assert (str/includes? (get the-keys :client-cert) "-----BEGIN CERTIFICATE-----"))
  (assert (str/includes? (get the-keys :client-cert) "-----END CERTIFICATE-----"))
  (assert (str/includes? (get the-keys :client-key) "-----BEGIN PRIVATE KEY-----"))
  (assert (str/includes? (get the-keys :client-key) "-----END PRIVATE KEY-----"))
  (str (get the-keys :ca-cert)
       (get the-keys :client-cert)
       (get the-keys :client-key)))

(defn certs-array-2 [opts]
  (let [the-keys (generate-certs opts)]
    [(server-keys the-keys) (client-keys the-keys)]))

(comment
  (server-keys (generate-certs {})))

(defn set-permissions! [f]
  (when f
    (let [f (io/file f)]
      (.setExecutable f false)
      (.setReadable f false false)
      (.setReadable f true true)
      (.setWritable f false))))

(defn write-certs! [{:keys [prefix server-out-file client-out-file]
                     :or {server-out-file "server.keys"
                          client-out-file "client.keys"}
                     :as opts}]
  (if (some? prefix)
    (write-certs! (-> opts
                      (dissoc :prefix)
                      (assoc :server-out-file (str prefix "-" "server.keys"))
                      (assoc :client-out-file (str prefix "-" "client.keys"))))
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
      (println "Wrote" client-out-file))))
