# zio-jwt-validator

[![CI/CD](https://github.com/janlisse/zio-jwt-validator/actions/workflows/ci.yml/badge.svg)](https://github.com/janlisse/zio-jwt-validator/actions/workflows/ci.yml)

A [ZIO](https://zio.dev/) based library for validating JWT tokens. Includes fetching keys from JWKS and claim validation.

### What this is
You want to protect your Scala backend API using signed JWT tokens?  
You don't want to hardcode public keys needed for token signature verification?  
You want to use JWKS over HTTPS to retrieve a matching public key?  
You want to perform additional validation of JWT claims, e.g. `audience` and `issuer`?  
You want a Scala library that implements all of this?  
Then `zio-jwt-validator` might be for you.

It is based on zio, [zio-json](https://github.com/zio/zio-json), [zio-http](https://github.com/zio/zio-http) (for the http client) and [jwt-scala](https://jwt-scala.github.io/jwt-scala/jwt-core-jwt.html) for JWT token parsing
and signature verification.

### Status
- Only validation of RSA signed tokens is currently supported, which works fine with an Authentication Provider like
[Auth0](https://auth0.com/)
- Validation of elliptic curve signatures will be added soon
- Symmetric keys are not supported for the time being

### Usage
Here is a simple program to validate a JWT token using `JwtValidator`:
```scala
  val program = for {
    _ <- JwtValidator.validate(jwtToken)
    _ <- ZIO.logInfo("Successfully validated token.")
  } yield ()
```
All sorts of possible validation errors will be returned through the error type of the returned ZIO.
There is a hierarchy of error types derived from `JwtValidationError`.

In order to run this program you need to provide a [ZLayer](https://zio.dev/reference/contextual/zlayer/) for a `JwtValidator`
and a `JwksFetcher`. Note: It is recommended to use the cached version of JwksFetcher which caches the retrieved JWKS, via: `JwksFetcherLive.cached(...)`.
It allows to configure a custom `cacheTTL`, the default is 10 minutes.
If you don't want any chaching at all you can still use: `JwksFetcherLive.uncached(...)`

```scala
  val run = program.provide(
    JwksFetcherLive.cached(
      "/Users/jan/.sdkman/candidates/java/current/lib/security/cacerts",
      "changeit"
    ),
    JwtValidatorLive.layer("https://your.auth.provider/.well-known/jwks.json",)
  )
```






