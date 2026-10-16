
const DEFAULT_API_BASE_URL = "https://traceback-platform.onrender.com";

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
  register: "/auth/register",
  login: "/auth/login",
  upload: "/api/images/upload",

  // Existing Watchlist API
  watchlist: "/api/watchlist",

  // Existing Inbox API
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

      if (response.status === 401) {
        throw new Error(
          "Please sign in again to continue."
        );
      }

      if (response.status === 403) {
        throw new Error(
          "You do not have permission for this action. " +
          "Please check your account and sign in again."
        );
      }

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

  return apiRequest(endpoint, {
    method: "POST",
    body: JSON.stringify(item),
  });
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

export function authenticate(mode, values) {
  const endpoint = API_ENDPOINTS[mode];

  if (!endpoint || !["login", "register"].includes(mode)) {
    throw new Error("Unsupported authentication mode.");
  }

  return apiRequest(endpoint, {
    method: "POST",
    body: JSON.stringify(values),
  });
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

  return apiRequest(API_ENDPOINTS.watchlist, {
    method: "POST",
    body: JSON.stringify({
      itemType: String(itemType).toUpperCase(),
      itemId,
    }),
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
    `${API_ENDPOINTS.watchlist}/${encodeURIComponent(watchlistItemId)}`,
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

  return apiRequest(API_ENDPOINTS.inbox, {
    method: "POST",
    body: JSON.stringify({
      itemType: String(itemType).toUpperCase(),
      itemId,
      message,
    }),
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

  return apiRequest(
    API_ENDPOINTS.conversationMessages(conversationId),
    {
      method: "POST",
      body: JSON.stringify({ content: message }),
    }
  );
}
