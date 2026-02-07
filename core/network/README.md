# Core Network Module

## Purpose
`core/network` provides shared networking infrastructure for all feature modules.  
Features should own endpoint interfaces and DTOs, while this module owns transport behavior, parsing, auth refresh orchestration, and error taxonomy.

## Package Layout
- `auth/`: token contracts and `RefreshTokenAuthenticator`.
- `client/`: base URL provider, `OkHttpClient` factory, Retrofit factory.
- `error/`: `ApiException` and local network error codes/statuses.
- `execution/`: `NetworkCallExecutor` for envelope/error parsing + transport mapping.
- `interceptor/`: request id and auth header interceptors.
- `model/`: shared response/headers/host models.
- `serialization/`: JSON parser helpers and default `kotlinx.serialization` config.
- `telemetry/`: request outcome observer contracts (`NetworkTelemetryObserver`).

## Current Request Flow
1. Feature builds Retrofit API from `RetrofitServiceFactory`.
2. Request passes interceptors (`RequestIdInterceptor`, `AuthorizationHeaderInterceptor`).
3. `RetryPolicyInterceptor` handles retryable transport failures and `429/5xx` responses.
4. Unauthorized responses may trigger `RefreshTokenAuthenticator` retry (idempotency-safe policy).
5. `NetworkCallExecutor` maps response envelopes and failures into `ApiResponse<T>`.
6. Optional `throwOnError=true` converts error responses to `ApiException`.

## Usage Rules
- Do not add feature-specific DTOs/endpoints in `core/network`; keep them in `feature/*/data/remote`.
- For write retries, include `Idempotency-Key` header.
- Keep parser logic deterministic and side-effect free.
- Preserve request correlation via `X-Request-Id`.
- Do not rely on OkHttp implicit retries; retries are explicitly controlled via `RetryPolicyInterceptor`.
- `prod` transport must use strict certificate pinning with primary + backup pins (rotation-ready).
- `dev` transport may stay relaxed for local/backend-core-kit workflows.

## Minimal Usage Example
```kotlin
val authApi = retrofitServiceFactory.create<AuthApi>(ApiHost.AUTH)
val response = networkCallExecutor.execute(
    call = { authApi.login(requestBody) },
    parser = AuthResponseModel.fromJson,
    throwOnError = false,
)
```

## Next Milestone
See `_WIP/2026-02-07_pr2_network_hardening_plan.md` for production hardening tasks before broad endpoint expansion.
