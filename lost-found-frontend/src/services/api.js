const DEFAULT_API_BASE_URL =
  "https://traceback-platform.onrender.com";

const API_BASE_URL = (
  import.meta.env.VITE_API_URL || DEFAULT_API_BASE_URL
).replace(/\/+$/, "");

export const API_ENDPOINTS = {
  lost: "/api/lost-items",
  found: "/api/found-items",
  categories: "/api/categories",
  matches: "/api/matches/my",
  confirmMatch: (id) => `/api/matches/${id}/confirm`,
  dismissMatch: (id) => `/api/matches/${id}/dismiss`,

  // Legacy endpoint: direct registration is disabled by the backend.
  register: "/auth/register",
  login: "/auth/login",

  // OTP registration.
  registerRequestOtp: "/auth/register/request-otp",
  registerVerifyOtp: "/auth/register/verify-otp",

  // OTP password recovery.
  passwordResetRequestOtp: "/auth/password/forgot/request-otp",
  passwordResetVerifyOtp: "/auth/password/forgot/verify-otp",
  passwordReset: "/auth/password/forgot/reset",

  upload: "/api/images/upload",

  watchlist: "/api/watchlist",

  inbox: "/api/inbox/conversations",
  conversationMessages: (id) =>
    `/api/inbox/conversations/${id}/messages`,
};

export function getApiBaseUrl() {
  return API_BASE_URL;
}

export async function apiRequest(path, options = {}) {
  const controller = new AbortController();
  const timeoutMs = options.timeoutMs ?? 15000;

  const timeoutId = setTimeout(
    () => controller.abort(),
    timeoutMs
  );

  const { timeoutMs: _ignored, ...fetchOptions } = options;
  const token = localStorage.getItem("lfm_token");
  const headers = new Headers(fetchOptions.headers || {});

  if (token) {
    headers.set("Authorization", `Bearer ${token}`);
  }

  const isFormData =
    typeof FormData !== "undefined" &&
    fetchOptions.body instanceof FormData;

  if (
    fetchOptions.body &&
    !isFormData &&
    !headers.has("Content-Type")
  ) {
    headers.set("Content-Type", "application/json");
  }

  try {
    const response = await fetch(`${API_BASE_URL}${path}`, {
      ...fetchOptions,
      headers,
      signal: controller.signal,
    });

    const responseText = await response.text();
    let data = null;

    if (responseText) {
      try {
        data = JSON.parse(responseText);
      } catch {
        data = responseText;
      }
    }

    if (!response.ok) {
      const message =
        typeof data === "string"
          ? data
          : data?.message ||
            data?.detail ||
            data?.error ||
            `Request failed (${response.status})`;

      throw new Error(message);
    }

    return data;
  } catch (error) {
    if (error.name === "AbortError") {
      throw new Error(
        "The server took too long to respond. Please retry in a moment."
      );
    }

    if (error instanceof TypeError) {
      throw new Error(
        "Cannot connect to the server. It may be starting up; please retry shortly."
      );
    }

    throw error;
  } finally {
    clearTimeout(timeoutId);
  }
}

function postJson(path, values) {
  return apiRequest(path, {
    method: "POST",
    body: JSON.stringify(values),
  });
}

// ==================== ITEMS API ====================

export function getItems(type) {
  const endpoint = API_ENDPOINTS[type];

  if (!endpoint || !["lost", "found"].includes(type)) {
    throw new Error(`Unknown item type: ${type}`);
  }

  return apiRequest(endpoint);
}

export function getCategories() {
  return apiRequest(API_ENDPOINTS.categories);
}

export function reportItem(type, item) {
  const endpoint = API_ENDPOINTS[type];

  if (!endpoint || !["lost", "found"].includes(type)) {
    throw new Error("Unknown report type.");
  }

  return postJson(endpoint, item);
}

export function uploadItemImage(file) {
  if (!(file instanceof File)) {
    throw new Error("Please select a valid image file.");
  }

  const body = new FormData();
  body.append("file", file);

  return apiRequest(API_ENDPOINTS.upload, {
    method: "POST",
    body,
  });
}

// ==================== AUTHENTICATION API ====================

// Login continues to use the existing endpoint.
// For mobile login, pass the phone number in the `email` field
// because that is the existing LoginRequest property name.
export function authenticate(mode, values) {
  const endpoint = API_ENDPOINTS[mode];

  if (!endpoint || !["login", "register"].includes(mode)) {
    throw new Error("Unsupported authentication mode.");
  }

  return postJson(endpoint, values);
}

// Register: request a code before creating an account.
export function requestRegistrationOtp(values) {
  return postJson(API_ENDPOINTS.registerRequestOtp, values);
}

// Register: verify code and create the account.
export function verifyRegistrationOtp(values) {
  return postJson(API_ENDPOINTS.registerVerifyOtp, values);
}

// Forgot password: request an email or mobile OTP.
export function requestPasswordResetOtp(values) {
  return postJson(
    API_ENDPOINTS.passwordResetRequestOtp,
    values
  );
}

// Forgot password: verify the OTP and receive a reset token.
export function verifyPasswordResetOtp(values) {
  return postJson(
    API_ENDPOINTS.passwordResetVerifyOtp,
    values
  );
}

// Forgot password: submit the reset token and new password.
export function resetPassword(values) {
  return postJson(API_ENDPOINTS.passwordReset, values);
}

// ==================== MATCHES API ====================

export function getMyMatches() {
  return apiRequest(API_ENDPOINTS.matches);
}

export function confirmMatch(id) {
  return apiRequest(API_ENDPOINTS.confirmMatch(id), {
    method: "PATCH",
  });
}

export function dismissMatch(id) {
  return apiRequest(API_ENDPOINTS.dismissMatch(id), {
    method: "PATCH",
  });
}

// ==================== WATCHLIST API ====================

export function getMyWatchlist() {
  return apiRequest(API_ENDPOINTS.watchlist);
}

export function saveWatchlistItem(itemType, itemId) {
  if (!["LOST", "FOUND"].includes(String(itemType).toUpperCase())) {
    throw new Error("Item type must be LOST or FOUND.");
  }

  if (itemId === undefined || itemId === null || itemId === "") {
    throw new Error("A valid item ID is required.");
  }

  return postJson(API_ENDPOINTS.watchlist, {
    itemType: String(itemType).toUpperCase(),
    itemId,
  });
}

export function removeWatchlistItem(watchlistItemId) {
  if (
    watchlistItemId === undefined ||
    watchlistItemId === null ||
    watchlistItemId === ""
  ) {
    throw new Error("A valid Watchlist entry ID is required.");
  }

  return apiRequest(
    `${API_ENDPOINTS.watchlist}/${encodeURIComponent(
      watchlistItemId
    )}`,
    {
      method: "DELETE",
    }
  );
}

// ==================== INBOX API ====================

export function getInbox() {
  return apiRequest(API_ENDPOINTS.inbox);
}

export function startConversation(itemType, itemId, message) {
  if (!["LOST", "FOUND"].includes(String(itemType).toUpperCase())) {
    throw new Error("Item type must be LOST or FOUND.");
  }

  return postJson(API_ENDPOINTS.inbox, {
    itemType: String(itemType).toUpperCase(),
    itemId,
    message,
  });
}

export function getConversationMessages(conversationId) {
  return apiRequest(
    API_ENDPOINTS.conversationMessages(conversationId)
  );
}

export function sendInboxMessage(conversationId, content) {
  const message = String(content ?? "").trim();

  if (!message) {
    throw new Error("Please enter a message.");
  }

  return postJson(
    API_ENDPOINTS.conversationMessages(conversationId),
    { content: message }
  );
}