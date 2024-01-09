# <img align="right" src="locksmith.png" width="210" height="291"> Locksmith

Generate certificates and keys easily. A tiny wrapper over the excellent [okhttp-tls](https://github.com/square/okhttp/tree/master/okhttp-tls).
To be used together with [nREPL](https://github.com/nrepl/nrepl).

## Installation

```bash
clojure -Ttools install com.github.ivarref/locksmith '{:git/tag "0.1.9"}' :as locksmith
```

## Usage

```bash
clojure -Tlocksmith write-certs!
```
