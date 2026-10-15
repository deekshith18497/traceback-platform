
import { useCallback, useEffect, useMemo, useState } from "react";
import "./App.css";

import {
  authenticate,
  getApiBaseUrl,
  getCategories,
  getItems,
  reportItem,
  uploadItemImage,
  getMyMatches,
  confirmMatch,
  dismissMatch,
  getInbox,
  startConversation,
  getConversationMessages,
  sendInboxMessage,
  getMyWatchlist,
  saveWatchlistItem,
  removeWatchlistItem,
} from "./services/api";

const TODAY = new Date().toISOString().slice(0, 10);

const EMPTY_FORM = {
  title: "",
  description: "",
  categoryId: "",
  location: "",
  date: TODAY,
  latitude: null,
  longitude: null,
  imageFile: null,
  imageUrl: "",
  imagePreview: "",
};

const FALLBACK_CATEGORIES = [
  "Electronics",
  "Bags",
  "Documents",
  "Keys",
  "Clothing",
  "Accessories",
  "Other",
];

function rows(data) {
  if (Array.isArray(data)) return data;
  if (Array.isArray(data?.content)) return data.content;
  if (Array.isArray(data?.items)) return data.items;
  if (Array.isArray(data?.data)) return data.data;
  return [];
}

function Brand() {
  return (
    <a className="brand" href="#home">
      <span className="brand-mark">✦</span>
      <span>
        HERO<span className="brand-red">FIND</span>
      </span>
      <small>LOST &amp; FOUND INITIATIVE</small>
    </a>
  );
}

export default function App() {
  const [lostItems, setLostItems] = useState([]);
  const [foundItems, setFoundItems] = useState([]);
  const [categories, setCategories] = useState([]);

  const [matches, setMatches] = useState([]);
  const [watchlist, setWatchlist] = useState([]);
  const [watchlistLoading, setWatchlistLoading] = useState(false);
  const [watchlistBusyId, setWatchlistBusyId] = useState(null);
  const [inbox, setInbox] = useState([]);
  const [inboxLoading, setInboxLoading] = useState(false);
  const [inboxError, setInboxError] = useState("");
  const [activeConversation, setActiveConversation] = useState(null);
  const [conversationMessages, setConversationMessages] = useState([]);
  const [messagesLoading, setMessagesLoading] = useState(false);
  const [messageText, setMessageText] = useState("");
  const [inboxBusy, setInboxBusy] = useState(false);

  const [matchesLoading, setMatchesLoading] = useState(false);
  const [matchesError, setMatchesError] = useState("");
  const [matchActionId, setMatchActionId] = useState(null);

  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState("");
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");

  const [activeTab, setActiveTab] = useState("all");
  const [search, setSearch] = useState("");
  const [categoryFilter, setCategoryFilter] = useState("all");

  const [modal, setModal] = useState("");
  const [authMode, setAuthMode] = useState("login");

  const [authForm, setAuthForm] = useState({
    name: "",
    email: "",
    phone: "",
    password: "",
  });

  const passwordChecks = [
    {
      label: "At least 8 characters",
      valid: authForm.password.length >= 8,
    },
    {
      label: "One uppercase letter (A-Z)",
      valid: /[A-Z]/.test(authForm.password),
    },
    {
      label: "One lowercase letter (a-z)",
      valid: /[a-z]/.test(authForm.password),
    },
    {
      label: "One number (0-9)",
      valid: /[0-9]/.test(authForm.password),
    },
    {
      label: "One special character (!@#$...)",
      valid: /[^A-Za-z0-9]/.test(authForm.password),
    },
  ];

  const strongPassword = passwordChecks.every(
    (check) => check.valid
  );

  const passwordStrength = passwordChecks.filter(
    (check) => check.valid
  ).length;

  const [user, setUser] = useState(() => {
    try {
      return JSON.parse(localStorage.getItem("lfm_user") || "null");
    } catch {
      return null;
    }
  });

  const [reportType, setReportType] = useState("lost");
  const [form, setForm] = useState(EMPTY_FORM);
  const [submitting, setSubmitting] = useState(false);
  const [authBusy, setAuthBusy] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [aiAnalyzing, setAiAnalyzing] = useState(false);
  const [aiAnalysis, setAiAnalysis] = useState(null);
  const [geoMessage, setGeoMessage] = useState("");

  const loadWatchlist = useCallback(async () => {
    if (!localStorage.getItem("lfm_token")) {
      setWatchlist([]);
      return;
    }

    setWatchlistLoading(true);

    try {
      const data = await getMyWatchlist();
      setWatchlist(Array.isArray(data) ? data : []);
    } catch (err) {
      setError(err.message || "Unable to load saved items.");
    } finally {
      setWatchlistLoading(false);
    }
  }, []);

  const handleWatchlistToggle = useCallback(
    async (item) => {
      const token = localStorage.getItem("lfm_token");

      if (!token) {
        setNotice("Please sign in to save items.");
        setAuthMode("login");
        setModal("auth");
        return;
      }

      const itemId = item.id ?? item.itemId;
      const itemType =
        String(item._type || item.itemType || "").toUpperCase();

      if (!itemId || !["LOST", "FOUND"].includes(itemType)) {
        setError("Unable to identify this item. Please refresh and try again.");
        return;
      }

      const existing = watchlist.find(
        (saved) =>
          String(saved.itemId) === String(itemId) &&
          String(saved.itemType).toUpperCase() === itemType
      );

      const busyKey = `${itemType}-${itemId}`;
      setWatchlistBusyId(busyKey);
      setError("");
      setNotice("");

      try {
        if (existing) {
          const savedEntryId = existing.id ?? existing.watchlistId;

          if (savedEntryId == null) {
            await loadWatchlist();
            setError("Could not identify the saved entry. Please try again.");
            return;
          }

          await removeWatchlistItem(savedEntryId);
          setWatchlist((current) =>
            current.filter(
              (saved) =>
                String(saved.id ?? saved.watchlistId) !==
                String(savedEntryId)
            )
          );
          setNotice("Item removed from your Watchlist.");
        } else {
          await saveWatchlistItem(itemType, itemId);
          await loadWatchlist();
          setNotice("Item saved to your Watchlist.");
        }
      } catch (err) {
        setError(err.message || "Unable to update your Watchlist.");
      } finally {
        setWatchlistBusyId(null);
      }
    },
    [watchlist, loadWatchlist]
  );

  const loadItems = useCallback(async () => {
    setLoading(true);
    setLoadError("");

    const results = await Promise.allSettled([
      getItems("lost"),
      getItems("found"),
      getCategories(),
    ]);

    const lost = results[0];
    const found = results[1];
    const cats = results[2];

    if (lost.status === "fulfilled") {
      setLostItems(rows(lost.value));
    } else {
      setLostItems([]);
    }

    if (found.status === "fulfilled") {
      setFoundItems(rows(found.value));
    } else {
      setFoundItems([]);
    }

    if (cats.status === "fulfilled") {
      setCategories(rows(cats.value));
    } else {
      setCategories([]);
    }

    if (
      lost.status === "rejected" &&
      found.status === "rejected"
    ) {
      setLoadError(
        `${lost.reason?.message || "Lost reports unavailable"} ${found.reason?.message || "Found reports unavailable"
          }`.trim()
      );
    } else if (
      lost.status === "rejected" ||
      found.status === "rejected"
    ) {
      setLoadError(
        "Some community reports could not be loaded. You can retry."
      );
    }

    setLoading(false);
  }, []);

  const loadMatches = useCallback(async () => {
    if (!localStorage.getItem("lfm_token")) {
      setMatches([]);
      setMatchesError("");
      setMatchesLoading(false);
      return;
    }

    setMatchesLoading(true);
    setMatchesError("");

    try {
      const data = await getMyMatches();
      setMatches(rows(data));
    } catch (err) {
      setMatchesError(
        err.message || "Unable to load your matches."
      );
    } finally {
      setMatchesLoading(false);
    }
  }, []);

  const loadInbox = useCallback(async () => {
    if (!localStorage.getItem("lfm_token")) {
      setInbox([]);
      return;
    }

    setInboxLoading(true);
    setInboxError("");

    try {
      const data = await getInbox();
      setInbox(rows(data));
    } catch (err) {
      setInboxError(err.message || "Unable to load your inbox.");
    } finally {
      setInboxLoading(false);
    }
  }, []);


  function getItemIllustration(title = "") {
    const item = title
      .toLowerCase()
      .replace(/[^a-z0-9\s-]/g, " ")
      .replace(/[-\s]+/g, " ")
      .trim();

    const categories = [
      { words: ["black pen", "blue pen", "red pen", "ball pen", "ballpoint", "pen", "pencil", "marker", "highlighter", "eraser", "sharpener", "stationery"], emoji: "🖊️" },
      { words: ["backpack", "school bag", "schoolbag", "laptop bag", "handbag", "travel bag", "suitcase", "bag", "rucksack"], emoji: "🎒" },
      { words: ["smartphone", "mobile phone", "cell phone", "iphone", "android phone", "mobile", "phone"], emoji: "📱" },
      { words: ["laptop", "notebook computer", "macbook", "computer", "desktop", "keyboard", "mouse"], emoji: "💻" },
      { words: ["water bottle", "bottle", "flask", "thermos", "sipper"], emoji: "🧴" },
      { words: ["textbook", "notebook", "diary", "journal", "book", "novel", "text book"], emoji: "📚" },
      { words: ["wallet", "purse", "coin purse", "card holder"], emoji: "👛" },
      { words: ["keychain", "key chain", "house key", "car key", "keys", "key"], emoji: "🔑" },
      { words: ["smartwatch", "wristwatch", "watch", "fitness band"], emoji: "⌚" },
      { words: ["headphones", "headphone", "earphones", "earphone", "earbuds", "airpods"], emoji: "🎧" },
      { words: ["umbrella", "raincoat"], emoji: "☂️" },
      { words: ["sneakers", "sports shoes", "shoe", "shoes", "sandals", "slippers", "footwear"], emoji: "👟" },
      { words: ["charger", "charging cable", "usb cable", "adapter", "power bank", "extension cord"], emoji: "🔌" },
      { words: ["spectacles", "eyeglasses", "sunglasses", "glasses", "eyewear"], emoji: "👓" },
      { words: ["identity card", "id card", "college id", "student id", "badge", "id"], emoji: "🪪" },
      { words: ["lunch box", "lunchbox", "tiffin", "food container"], emoji: "🍱" },
      { words: ["t-shirt", "shirt", "jacket", "hoodie", "sweater", "clothing", "uniform"], emoji: "👕" },
      { words: ["ring", "necklace", "bracelet", "jewellery", "jewelry", "chain", "earring"], emoji: "💍" },
      { words: ["football", "soccer ball"], emoji: "⚽" },
      { words: ["cricket bat", "cricket ball", "bat", "stumps"], emoji: "🏏" },
      { words: ["tennis racket", "badminton racket", "racket", "shuttlecock"], emoji: "🏸" },
      { words: ["helmet", "bike helmet"], emoji: "🪖" },
      { words: ["cycle", "bicycle", "cycling helmet"], emoji: "🚲" },
      { words: ["camera", "dslr", "digital camera"], emoji: "📷" },
      { words: ["tablet", "ipad"], emoji: "📲" },
      { words: ["passport", "document", "certificate", "papers", "file folder"], emoji: "📄" },
      { words: ["toy", "teddy bear", "stuffed animal", "doll"], emoji: "🧸" },
      { words: ["medicine box", "first aid", "medical kit"], emoji: "🩹" },
      { words: ["comb", "hairbrush", "hair brush"], emoji: "💇" },
      { words: ["makeup", "cosmetics", "lipstick"], emoji: "💄" },
      { words: ["calculator"], emoji: "🧮" },
      { words: ["charger case", "airpods case", "phone case"], emoji: "📦" },
      { words: ["dog", "puppy", "pet"], emoji: "🐕" },
      { words: ["lunch", "snack box"], emoji: "🥪" },
      { words: ["keypad", "remote control", "tv remote", "remote"], emoji: "🎛️" },
      { words: ["microphone", "speaker", "music player"], emoji: "🎤" },
      { words: ["socks"], emoji: "🧦" },
      { words: ["cap", "hat"], emoji: "🧢" },
      { words: ["handbag", "shopping bag"], emoji: "👜" },
      { words: ["gift", "present"], emoji: "🎁" },
    ];

    // Match the most specific item description first.
    const sortedCategories = [...categories].sort(
      (a, b) =>
        Math.max(...b.words.map((word) => word.length)) -
        Math.max(...a.words.map((word) => word.length))
    );

    const match = sortedCategories.find(({ words }) =>
      words.some((word) =>
        new RegExp(`\\b${word.replace(/[.*+?^${}()|[\]\\]/g, "\\$&")}\\b`, "i").test(item)
      )
    );

    return match?.emoji || "📦";
  }



  async function openConversation(conversation) {
    if (!conversation?.id) {
      setInboxError("Unable to open this conversation. Please refresh your inbox.");
      return;
    }

    setActiveConversation(conversation);
    setConversationMessages([]);
    setInboxError("");
    setMessagesLoading(true);

    try {
      const data = await getConversationMessages(conversation.id);

      // Ignore this result if another conversation was selected.
      setConversationMessages(rows(data));
      await loadInbox();
    } catch (err) {
      setInboxError(err.message || "Unable to load messages.");
    } finally {
      setMessagesLoading(false);
    }
  }

  async function contactOwner(item) {
    if (!localStorage.getItem("lfm_token")) {
      setAuthMode("login");
      setModal("auth");
      setError("");
      setNotice("Sign in to contact the person who reported this item.");
      return;
    }

    const message = window.prompt(
      `Write your first message about "${item.title}":`
    );

    if (!message || !message.trim()) return;

    setInboxBusy(true);
    setInboxError("");
    setError("");

    try {
      const conversation = await startConversation(
        item._type.toUpperCase(),
        item.id,
        message.trim()
      );

      await loadInbox();
      await openConversation(conversation);
      setNotice("Conversation started successfully.");
    } catch (err) {
      setInboxError(err.message || "Unable to contact the item owner.");
    } finally {
      setInboxBusy(false);
    }
  }

  async function handleSendMessage(event) {
    event.preventDefault();

    if (!activeConversation || !messageText.trim() || inboxBusy) {
      return;
    }

    setInboxBusy(true);
    setInboxError("");

    try {
      await sendInboxMessage(
        activeConversation.id,
        messageText.trim()
      );

      setMessageText("");

      const data = await getConversationMessages(activeConversation.id);
      setConversationMessages(rows(data));
      await loadInbox();
    } catch (err) {
      setInboxError(err.message || "Unable to send message.");
    } finally {
      setInboxBusy(false);
    }
  }



  useEffect(() => {
    loadItems();
  }, [loadItems]);

  useEffect(() => {
    loadInbox();
  }, [loadInbox, user]);

  // Load the Watchlist independently of Inbox selection.
  useEffect(() => {
    if (!user) {
      setWatchlist([]);
      setWatchlistLoading(false);
      return;
    }

    loadWatchlist();
  }, [loadWatchlist, user]);

  // Refresh messages only for the currently selected conversation.
  useEffect(() => {
    const conversationId = activeConversation?.id;

    if (!conversationId) {
      setConversationMessages([]);
      setMessagesLoading(false);
      return;
    }

    let cancelled = false;

    const refreshMessages = async () => {
      try {
        const data = await getConversationMessages(conversationId);

        if (!cancelled) {
          setConversationMessages(rows(data));
          setInboxError("");
        }
      } catch (err) {
        if (!cancelled) {
          setInboxError(
            err.message || "Unable to refresh messages."
          );
        }
      }
    };

    const intervalId = window.setInterval(refreshMessages, 5000);

    return () => {
      cancelled = true;
      window.clearInterval(intervalId);
    };
  }, [activeConversation?.id]);

  const allItems = useMemo(
    () => [
      ...lostItems.map((item) => ({
        ...item,
        _type: "lost",
        _categoryName:
          item.categoryName || item.category?.name || "Other",
        _date: item.lostDate,
      })),
      ...foundItems.map((item) => ({
        ...item,
        _type: "found",
        _categoryName:
          item.categoryName || item.category?.name || "Other",
        _date: item.foundDate,
      })),
    ],
    [lostItems, foundItems]
  );

  const filteredItems = useMemo(
    () =>
      allItems.filter((item) => {
        const text = `${item.title || ""} ${item.description || ""
          } ${item._categoryName} ${item.location || ""
          }`.toLowerCase();

        return (
          (activeTab === "all" || item._type === activeTab) &&
          (!search.trim() ||
            text.includes(search.trim().toLowerCase())) &&
          (categoryFilter === "all" ||
            item._categoryName.toLowerCase() ===
            categoryFilter.toLowerCase())
        );
      }),
    [allItems, activeTab, search, categoryFilter]
  );

  function openReport(type) {
    setError("");
    setNotice("");
    setReportType(type);
    setForm({ ...EMPTY_FORM });
    setAiAnalysis(null);
    setGeoMessage("");

    if (!localStorage.getItem("lfm_token")) {
      setModal("auth");
      setAuthMode("login");
      setNotice(
        "Sign in or create an account to submit a secure report."
      );
      return;
    }

    setModal("report");
  }

  function updateReport(event) {
    const { name, value } = event.target;

    setForm((prev) => ({
      ...prev,
      [name]: value,
    }));
  }

  function updateAuth(event) {
    const { name, value } = event.target;

    setAuthForm((prev) => ({
      ...prev,
      [name]: value,
    }));
  }

  async function handleAuth(event) {
    event.preventDefault();

    if (authBusy) return;

    setError("");
    setNotice("");

    if (authMode === "register") {
      if (authForm.name.trim().length < 3) {
        setError(
          "Full name must contain at least 3 characters."
        );
        return;
      }

      if (!strongPassword) {
        setError(
          "Please create a strong password using all five requirements."
        );
        return;
      }
    }

    setAuthBusy(true);

    try {
      const result = await authenticate(
        authMode,
        authMode === "register"
          ? {
            name: authForm.name.trim(),
            email: authForm.email.trim(),
            phone: authForm.phone.trim(),
            password: authForm.password,
          }
          : {
            email: authForm.email.trim(),
            password: authForm.password,
          }
      );

      if (!result?.token) {
        throw new Error(
          "The server did not return a sign-in token."
        );
      }

      localStorage.setItem("lfm_token", result.token);

      const safeUser = {
        id: result.userId,
        name: result.name,
        email: result.email,
        role: result.role,
      };

      localStorage.setItem(
        "lfm_user",
        JSON.stringify(safeUser)
      );

      setUser(safeUser);
      setModal("");

      setAuthForm({
        name: "",
        email: "",
        phone: "",
        password: "",
      });

      setNotice(
        `Welcome${result.name ? `, ${result.name}` : ""
        }! You are signed in.`
      );

      await loadMatches();
    } catch (err) {
      setError(
        err.message || "Authentication failed. Please try again."
      );
    } finally {
      setAuthBusy(false);
    }
  }

  function logout() {
    localStorage.removeItem("lfm_token");
    localStorage.removeItem("lfm_user");

    setUser(null);
    setMatches([]);
    setMatchesError("");
    setNotice("You have signed out.");
    setModal("");
  }

  async function handleMatchAction(matchId, action) {
    setMatchActionId(matchId);
    setMatchesError("");
    setNotice("");
    setError("");

    try {
      if (action === "confirm") {
        await confirmMatch(matchId);
        setNotice("Match confirmed successfully.");
      } else {
        await dismissMatch(matchId);
        setNotice("Match dismissed.");
      }

      await loadMatches();
    } catch (err) {
      setMatchesError(
        err.message || "Unable to update this match."
      );
    } finally {
      setMatchActionId(null);
    }
  }

  function getCurrentLocation() {
    setGeoMessage("");

    if (!navigator.geolocation) {
      setGeoMessage(
        "Location is not supported by this browser. Enter the place manually."
      );
      return;
    }

    setGeoMessage("Waiting for location permission…");

    navigator.geolocation.getCurrentPosition(
      (position) => {
        setForm((prev) => ({
          ...prev,
          latitude: position.coords.latitude,
          longitude: position.coords.longitude,
          location:
            prev.location.trim() ||
            `${position.coords.latitude.toFixed(5)}, ${position.coords.longitude.toFixed(5)}`,
        }));

        setGeoMessage(
          "Location added. Your browser only shared it after permission."
        );
      },
      (err) => {
        setGeoMessage(
          err.code === 1
            ? "Location permission was denied. You can enter the place manually."
            : "Could not get location. Please enter the place manually."
        );
      },
      {
        enableHighAccuracy: true,
        timeout: 10000,
        maximumAge: 60000,
      }
    );
  }

  async function chooseImage(event) {
    const file = event.target.files?.[0];

    if (!file) return;

    if (!["image/jpeg", "image/png", "image/webp"].includes(file.type)) {
      setError("Choose a JPEG, PNG, or WebP image.");
      event.target.value = "";
      return;
    }

    if (file.size > 5 * 1024 * 1024) {
      setError("Image must be 5 MB or smaller.");
      event.target.value = "";
      return;
    }

    setError("");
    setNotice("");
    setAiAnalysis(null);
    setAiAnalyzing(true);

    const imagePreview = URL.createObjectURL(file);

    try {
      // Analyze first. Unsuitable images are not uploaded to item storage.
      const apiBaseUrl = getApiBaseUrl().replace(/\/$/, "");
      const formData = new FormData();
      formData.append("file", file);

      const token = localStorage.getItem("lfm_token");
      const response = await fetch(`${apiBaseUrl}/api/ai/analyze-image`, {
        method: "POST",
        headers: token ? { Authorization: `Bearer ${token}` } : {},
        body: formData,
      });

      let analysis;
      try {
        analysis = await response.json();
      } catch {
        throw new Error("AI analysis returned an unreadable response.");
      }

      if (!response.ok) {
        throw new Error(
          analysis.message || analysis.error ||
          `AI image analysis failed (${response.status}).`
        );
      }

      if (typeof analysis.suitable !== "boolean") {
        throw new Error(
          "AI response is missing the suitable field. Check the backend response."
        );
      }

      setAiAnalysis(analysis);

      if (!analysis.suitable) {
        setForm((prev) => ({
          ...prev,
          imageFile: file,
          imageUrl: "",
          imagePreview,
        }));
        setNotice("AI could not approve this photo. Choose another photo or remove it to continue manually.");
        return;
      }

      // Preserve the existing image-upload endpoint and storage flow.
      setUploading(true);
      const uploadResult = await uploadItemImage(file);
      const categoryMatch = categories.find(
        (category) =>
          String(category.name || "").toLowerCase() ===
          String(analysis.category || "Other").toLowerCase()
      );

      setForm((prev) => ({
        ...prev,
        title: prev.title.trim() || analysis.title || "",
        description: prev.description.trim() || analysis.description || "",
        categoryId: prev.categoryId || (categoryMatch ? String(categoryMatch.id) : ""),
        imageFile: file,
        imageUrl: uploadResult.imageUrl,
        imagePreview,
      }));

      setNotice(
        "AI identified the item and suggested report details. Review them before submitting."
      );
    } catch (err) {
      URL.revokeObjectURL(imagePreview);
      setAiAnalysis(null);
      setError(err.message || "Unable to analyze this image. Please try again.");
    } finally {
      setAiAnalyzing(false);
      setUploading(false);
      event.target.value = "";
    }
  }

  async function handleSubmit(event) {
    event.preventDefault();
    setError("");
    setNotice("");

    if (!localStorage.getItem("lfm_token")) {
      setError("Please sign in before submitting a report.");
      setModal("auth");
      return;
    }

    if (form.title.trim().length < 3) {
      setError("Item name must contain at least 3 characters.");
      return;
    }

    if (form.description.trim().length < 10) {
      setError(
        "Description must contain at least 10 characters."
      );
      return;
    }

    if (aiAnalysis && !aiAnalysis.suitable && form.imagePreview) {
      setError("This photo was marked unsuitable by AI. Choose another photo or remove it before submitting.");
      return;
    }

    if (!form.categoryId) {
      setError("Please select a category.");
      return;
    }

    if (form.location.trim().length < 3) {
      setError("Please enter a valid location.");
      return;
    }

    setSubmitting(true);

    const dateTime = `${form.date}T00:00:00`;

    const payload = {
      title: form.title.trim(),
      description: form.description.trim(),
      location: form.location.trim(),
      categoryId: Number(form.categoryId),
      latitude: form.latitude,
      longitude: form.longitude,
      imageUrl: form.imageUrl || null,
      ...(reportType === "lost"
        ? { lostDate: dateTime }
        : { foundDate: dateTime }),
    };

    try {
      await reportItem(reportType, payload);

      setModal("");
      setForm({ ...EMPTY_FORM });
      setAiAnalysis(null);

      setNotice(
        `Your ${reportType} item report was submitted successfully.`
      );

      await loadItems();
      await loadMatches();
    } catch (err) {
      setError(
        err.message || "Report submission failed. Please retry."
      );
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="app-shell marvel-shell" id="home">
      <div className="top-strip">
        EARTH'S MISSING ITEMS NEED A HERO <span>•</span>{" "}
        COMMUNITY RECOVERY NETWORK
      </div>

      <header className="topbar">
        <Brand />


        <nav className="nav-links">
          <a href="#home">HQ</a>
          <a href="#items">CASE FILES</a>
          <a href="#my-matches">MY MATCHES</a>
          <a href="#inbox">INBOX</a>
          <a href="#how-it-works">HOW IT WORKS</a>
        </nav>


        <div className="header-actions">
          {user ? (
            <>
              <span className="signed-in">
                AGENT {user.name?.split(" ")[0]?.toUpperCase()}
              </span>

              <button
                type="button"
                className="button button-outline"
                onClick={logout}
              >
                Sign out
              </button>
            </>
          ) : (
            <button
              type="button"
              className="button button-outline"
              onClick={() => {
                setAuthMode("login");
                setModal("auth");
                setError("");
                setNotice("");
              }}
            >
              Sign in
            </button>
          )}

          <button
            type="button"
            className="button button-primary nav-report"
            onClick={() => openReport("lost")}
          >
            + REPORT ITEM
          </button>
        </div>
      </header>

      <main>
        <section className="hero marvel-hero">
          <div className="hero-copy">
            <div className="eyebrow">
              <span className="live-dot" /> THE LOST &amp; FOUND
              INITIATIVE
            </div>

            <h1>
              NOT ALL HEROES
              <br />
              <span>WEAR CAPES.</span>
            </h1>

            <p>
              Sometimes, a hero is the person who finds a missing
              phone, returns a lost bag, or helps a stranger get
              something important back.
            </p>

            <div className="hero-actions">
              <button
                type="button"
                className="button button-primary"
                onClick={() => openReport("lost")}
              >
                REPORT LOST ITEM <span>→</span>
              </button>

              <button
                type="button"
                className="button button-light"
                onClick={() => openReport("found")}
              >
                I FOUND SOMETHING
              </button>
            </div>

            <div className="hero-note">
              <span>✦</span> Every report opens a new case. Every
              match can change a day.
            </div>
          </div>

          <div className="hero-art marvel-art" aria-hidden="true">
            <div className="hero-emblem">
              H<span>F</span>
            </div>

            <div className="hero-art-caption">
              FRIENDLY NEIGHBORHOOD
              <br />
              ITEM RECOVERY
            </div>

            <div className="comic-burst">
              CASE
              <br />
              OPEN!
            </div>
          </div>
        </section>

        <section className="stats-row">
          <div className="stat-card">
            <span className="stat-icon lost-icon">⌕</span>
            <div>
              <strong>{lostItems.length}</strong>
              <span>LOST CASES</span>
            </div>
          </div>

          <div className="stat-card">
            <span className="stat-icon found-icon">✓</span>
            <div>
              <strong>{foundItems.length}</strong>
              <span>FOUND ITEMS</span>
            </div>
          </div>

          <div className="stat-card">
            <span className="stat-icon match-icon">✦</span>
            <div>
              <strong>{allItems.length}</strong>
              <span>COMMUNITY CASES</span>
            </div>
          </div>
        </section>

        {notice && (
          <div className="notice" role="status">
            <span>{notice}</span>
            <button
              type="button"
              onClick={() => setNotice("")}
              aria-label="Dismiss notification"
            >
              ×
            </button>
          </div>
        )}

        {error && modal !== "auth" && modal !== "report" && (
          <div className="error-banner" role="alert">
            <span>{error}</span>
            <button
              type="button"
              onClick={() => setError("")}
              aria-label="Dismiss error"
            >
              ×
            </button>
          </div>
        )}

        {user && (
          <section className="matches-section" id="my-matches">
            <div className="section-heading">
              <div>
                <div className="eyebrow">
                  AUTOMATED CASE ANALYSIS
                </div>
                <h2>MY MATCHES</h2>
                <p>
                  Potential matches found by comparing your lost and
                  found reports.
                </p>
              </div>

              <button
                type="button"
                className="button button-outline"
                onClick={loadMatches}
                disabled={matchesLoading}
              >
                {matchesLoading
                  ? "SCANNING..."
                  : "↻ REFRESH MATCHES"}
              </button>
            </div>

            {matchesError && (
              <div className="error-banner" role="alert">
                <span>{matchesError}</span>
                <button
                  type="button"
                  onClick={() => setMatchesError("")}
                  aria-label="Dismiss match error"
                >
                  ×
                </button>
              </div>
            )}

            {matchesLoading && matches.length === 0 ? (
              <div className="empty-state">
                <div className="loader" />
                <h3>SCANNING OPEN CASES...</h3>
                <p>Checking your potential matches.</p>
              </div>
            ) : matches.length === 0 ? (
              <div className="empty-state">
                <div className="empty-icon">✦</div>
                <h3>NO MATCHES YET</h3>
                <p>
                  When a compatible lost and found report is
                  detected, it will appear here.
                </p>
              </div>
            ) : (
              <div className="matches-grid">
                {matches.map((match) => {
                  const score = Math.max(
                    0,
                    Math.min(
                      100,
                      Number(match.similarityScore) || 0
                    )
                  );

                  const status = String(
                    match.status || "PENDING"
                  ).toUpperCase();

                  return (
                    <article
                      className="match-card"
                      key={match.id}
                    >
                      <div className="match-card-heading">
                        <span className="match-label">
                          CASE #{match.id ?? "—"}
                        </span>

                        <span
                          className={`match-status ${status.toLowerCase()}`}
                        >
                          {status}
                        </span>
                      </div>

                      <div className="match-items">
                        <div>
                          <small>LOST ITEM</small>
                          <h3>
                            {match.lostItemTitle ||
                              `Lost item #${match.lostItemId}`}
                          </h3>
                        </div>

                        <span className="match-arrow">↔</span>

                        <div>
                          <small>FOUND ITEM</small>
                          <h3>
                            {match.foundItemTitle ||
                              `Found item #${match.foundItemId}`}
                          </h3>
                        </div>
                      </div>

                      <div className="match-score-row">
                        <span>Similarity score</span>
                        <strong>{score}%</strong>
                      </div>

                      <div
                        className="match-score-track"
                        role="progressbar"
                        aria-label="Similarity score"
                        aria-valuemin={0}
                        aria-valuemax={100}
                        aria-valuenow={score}
                      >
                        <div
                          className="match-score-fill"
                          style={{ width: `${score}%` }}
                        />
                      </div>

                      <p className="match-reason">
                        <strong>Why it matched:</strong>{" "}
                        {match.matchReason ||
                          "The system detected possible similarities."}
                      </p>

                      {match.createdAt && (
                        <p className="match-date">
                          Detected:{" "}
                          {String(match.createdAt)
                            .replace("T", " ")
                            .slice(0, 16)}
                        </p>
                      )}

                      {status === "PENDING" && (
                        <div className="match-actions">
                          <button
                            type="button"
                            className="button button-primary"
                            disabled={matchActionId !== null}
                            onClick={() =>
                              handleMatchAction(
                                match.id,
                                "confirm"
                              )
                            }
                          >
                            {matchActionId === match.id
                              ? "UPDATING..."
                              : "✓ CONFIRM MATCH"}
                          </button>

                          <button
                            type="button"
                            className="button button-outline"
                            disabled={matchActionId !== null}
                            onClick={() =>
                              handleMatchAction(
                                match.id,
                                "dismiss"
                              )
                            }
                          >
                            {matchActionId === match.id
                              ? "UPDATING..."
                              : "DISMISS"}
                          </button>
                        </div>
                      )}
                    </article>
                  );
                })}
              </div>
            )}
          </section>
        )}



        {user && (
          <section className="matches-section inbox-section" id="inbox">
            <div className="section-heading">
              <div>
                <div className="eyebrow">DIRECT COMMUNICATION</div>
                <h2>MY INBOX</h2>
                <p>Contact item owners and discuss returning lost belongings.</p>
              </div>

              <button
                type="button"
                className="button button-outline"
                onClick={loadInbox}
                disabled={inboxLoading}
              >
                {inboxLoading ? "LOADING..." : "↻ REFRESH INBOX"}
              </button>
            </div>

            {inboxError && (
              <div className="error-banner" role="alert">
                <span>{inboxError}</span>
                <button
                  type="button"
                  onClick={() => setInboxError("")}
                  aria-label="Dismiss inbox error"
                >
                  ×
                </button>
              </div>
            )}

            <div className="inbox-layout">
              <div className="inbox-conversation-list">
                <h3>CONVERSATIONS</h3>

                {inboxLoading && inbox.length === 0 ? (
                  <p>Loading conversations...</p>
                ) : inbox.length === 0 ? (
                  <div className="empty-state">
                    <h3>NO MESSAGES YET</h3>
                    <p>Contact an item owner to start a conversation.</p>
                  </div>
                ) : (
                  inbox.map((conversation) => (
                    <button
                      type="button"
                      key={conversation.id}
                      className={`inbox-conversation ${activeConversation?.id === conversation.id
                        ? "active"
                        : ""
                        }`}
                      onClick={() => openConversation(conversation)}
                    >
                      <strong>
                        {conversation.otherUserName || "Community member"}
                      </strong>
                      <span>
                        {conversation.itemTitle ||
                          `${conversation.itemType || "ITEM"} CASE`}
                      </span>
                      <small>
                        {conversation.lastMessage || "Open conversation"}
                      </small>
                    </button>
                  ))
                )}
              </div>

              <div className="inbox-chat">
                {!activeConversation ? (
                  <div className="empty-state">
                    <h3>SELECT A CONVERSATION</h3>
                    <p>Choose a conversation on the left to read and reply.</p>
                  </div>
                ) : (
                  <>
                    <div className="inbox-chat-heading">
                      <h3>
                        {activeConversation.otherUserName ||
                          "Community member"}
                      </h3>
                      <p>
                        {activeConversation.itemTitle ||
                          `${activeConversation.itemType || "ITEM"} CASE`}
                      </p>
                    </div>

                    {messagesLoading ? (
                      <p>Loading messages...</p>
                    ) : (
                      <div className="inbox-message-list">
                        {conversationMessages.length === 0 ? (
                          <p>No messages yet. Start the conversation below.</p>
                        ) : (
                          conversationMessages.map((message) => (
                            <div
                              key={message.id}
                              className={`inbox-message ${Number(message.senderId) === Number(user.id)
                                ? "mine"
                                : ""
                                }`}
                            >
                              <strong>
                                {message.senderName || "User"}
                              </strong>
                              <p>{message.content}</p>
                              {message.createdAt && (
                                <small>
                                  {String(message.createdAt)
                                    .replace("T", " ")
                                    .slice(0, 16)}
                                </small>
                              )}
                            </div>
                          ))
                        )}
                      </div>
                    )}

                    <form
                      className="inbox-send-form"
                      onSubmit={handleSendMessage}
                    >
                      <input
                        type="text"
                        value={messageText}
                        onChange={(event) =>
                          setMessageText(event.target.value)
                        }
                        placeholder="Write your message..."
                        maxLength={2000}
                        required
                      />
                      <button
                        type="submit"
                        className="button button-primary"
                        disabled={inboxBusy || !messageText.trim()}
                      >
                        {inboxBusy ? "SENDING..." : "SEND"}
                      </button>
                    </form>
                  </>
                )}
              </div>
            </div>
          </section>
        )}

        <section className="items-section" id="items">
          <div className="section-heading">
            <div>
              <div className="eyebrow">
                S.H.I.E.L.D. CASE BOARD
              </div>
              <h2>COMMUNITY CASE FILES</h2>
              <p>
                Search reports, compare photos, and help an item
                find its way home.
              </p>
            </div>

            <button
              type="button"
              className="button button-outline"
              onClick={() => openReport("found")}
            >
              + I FOUND SOMETHING
            </button>
          </div>

          <div className="search-panel">
            <label className="search-input-wrap">
              <span>⌕</span>
              <input
                type="search"
                placeholder="Search item, details, or location..."
                value={search}
                onChange={(event) => setSearch(event.target.value)}
              />
            </label>

            <select
              value={categoryFilter}
              onChange={(event) =>
                setCategoryFilter(event.target.value)
              }
              aria-label="Filter category"
            >
              <option value="all">All categories</option>
              {(categories.length
                ? categories.map((category) => category.name)
                : FALLBACK_CATEGORIES
              ).map((name) => (
                <option key={name} value={name}>
                  {name}
                </option>
              ))}
            </select>
          </div>

          <div className="tabs" role="tablist">
            {[
              ["all", "ALL CASES"],
              ["lost", "LOST ITEMS"],
              ["found", "FOUND ITEMS"],
            ].map(([value, label]) => (
              <button
                key={value}
                type="button"
                role="tab"
                aria-selected={activeTab === value}
                className={`tab ${activeTab === value ? "active" : ""
                  }`}
                onClick={() => setActiveTab(value)}
              >
                {label}
              </button>
            ))}

            <span className="result-count">
              {filteredItems.length} CASES
            </span>
          </div>

          {loading ? (
            <div className="empty-state">
              <div className="loader" />
              <h3>CONTACTING THE NETWORK...</h3>
              <p>Loading community cases.</p>
            </div>
          ) : loadError ? (
            <div className="empty-state">
              <div className="empty-icon">!</div>
              <h3>NETWORK CONNECTION FAILED</h3>
              <p>{loadError}</p>
              <button
                type="button"
                className="button button-primary"
                onClick={loadItems}
              >
                RETRY CONNECTION
              </button>
            </div>
          ) : filteredItems.length === 0 ? (
            <div className="empty-state">
              <div className="empty-icon">⌕</div>
              <h3>NO OPEN CASES FOUND</h3>
              <p>
                Try another search, or be the hero who starts a
                new report.
              </p>

              <div className="empty-actions">
                <button
                  type="button"
                  className="button button-primary"
                  onClick={() => openReport("lost")}
                >
                  REPORT LOST ITEM
                </button>

                <button
                  type="button"
                  className="button button-light"
                  onClick={() => openReport("found")}
                >
                  REPORT FOUND ITEM
                </button>
              </div>
            </div>
          ) : (
            <div className="items-grid">
              {filteredItems.map((item) => {
                const latitude = Number(item.latitude);
                const longitude = Number(item.longitude);

                const hasCoordinates =
                  item.latitude != null &&
                  item.longitude != null &&
                  Number.isFinite(latitude) &&
                  Number.isFinite(longitude) &&
                  latitude >= -90 &&
                  latitude <= 90 &&
                  longitude >= -180 &&
                  longitude <= 180 &&
                  !(
                    Math.abs(latitude) < 1 &&
                    Math.abs(longitude) < 1
                  );

                return (
                  <article
                    className="item-card"
                    key={`${item._type}-${item.id}`}
                  >
                    <div
                      className={`item-card-banner ${item._type === "found" ? "found" : ""
                        }`}
                    >

                      {item.imageUrl ? (
                        <img
                          className="item-photo"
                          src={
                            item.imageUrl.startsWith("http")
                              ? item.imageUrl
                              : `${getApiBaseUrl()}${item.imageUrl}`
                          }
                          alt={item.title || "Reported item"}
                          loading="lazy"
                        />
                      ) : (
                        <span
                          className="item-placeholder"
                          role="img"
                          aria-label={item.title || "Item illustration"}
                        >
                          {getItemIllustration(item.title)}
                        </span>
                      )}


                      <span
                        className={`item-status ${item._type === "found" ? "found" : ""
                          }`}
                      >
                        {item._type === "found"
                          ? "FOUND ITEM"
                          : "LOST ITEM"}
                      </span>

                      <span className="item-category">
                        {item._categoryName}
                      </span>
                    </div>

                    <div className="item-card-body">
                      <h3>{item.title}</h3>

                      <p className="item-description">
                        {item.description}
                      </p>

                      <div className="item-meta">
                        <span>⌖</span>
                        {item.location || "Location not specified"}
                      </div>

                      {item._date && (
                        <div className="item-meta">
                          <span>◷</span>
                          {String(item._date).slice(0, 10)}
                        </div>
                      )}

                      {hasCoordinates && (
                        <a
                          className="map-link"
                          target="_blank"
                          rel="noreferrer"
                          href={`https://www.google.com/maps?q=${latitude},${longitude}`}
                        >
                          OPEN LOCATION MAP ↗
                        </a>
                      )}


                      <div className="contact-owner-options">
                        <button
                          type="button"
                          className="button button-outline contact-owner-button"
                          disabled={inboxBusy}
                          onClick={() => contactOwner(item)}
                        >
                          💬 In-app Inbox
                        </button>

                        <button
                          type="button"
                          className="button button-outline contact-owner-button"
                          onClick={() => {
                            const email = item.userEmail;
                            if (!email) {
                              alert("Owner email is not available.");
                              return;
                            }

                            const subject = encodeURIComponent(
                              `Regarding your ${item._type || "item"} listing on HERO FIND`
                            );
                            const body = encodeURIComponent(
                              "Hello, I am contacting you regarding your item listed on HERO FIND."
                            );

                            window.location.href =
                              `mailto:${email}?subject=${subject}&body=${body}`;
                          }}
                        >
                          ✉️ Email Owner
                        </button>

                        <button
                          type="button"
                          className="button button-outline contact-owner-button"
                          onClick={() => {
                            const phone = item.userPhone;

                            if (!phone) {
                              alert("Owner phone number is not available.");
                              return;
                            }

                            const message = encodeURIComponent(
                              "Hello, I found your listing on HERO FIND and would like to contact you about the item."
                            );

                            window.open(
                              `https://wa.me/${String(phone).replace(/\D/g, "")}?text=${message}`,
                              "_blank",
                              "noopener,noreferrer"
                            );
                          }}
                        >
                          📱 WhatsApp Owner
                        </button>
                      </div>




                      <div className="item-card-footer">
                        <span className="category-label">
                          {item._categoryName}
                        </span>

                        <span className="report-label">
                          {item._type === "found"
                            ? "HELP FIND THE OWNER"
                            : "HELP FIND THIS ITEM"}
                        </span>

                        {(() => {
                          const itemId = item.id ?? item.itemId;
                          const itemType =
                            String(item._type || item.itemType || "").toUpperCase();

                          const isSaved = watchlist.some(
                            (saved) =>
                              String(saved.itemId) === String(itemId) &&
                              String(saved.itemType).toUpperCase() === itemType
                          );

                          const busyKey = `${itemType}-${itemId}`;

                          return (
                            <button
                              type="button"
                              className="watchlist-button"
                              disabled={watchlistBusyId === busyKey || watchlistLoading}
                              aria-pressed={isSaved}
                              onClick={() => handleWatchlistToggle(item)}
                            >
                              {watchlistBusyId === busyKey
                                ? "Saving..."
                                : isSaved
                                  ? "★ Saved"
                                  : "☆ Save Item"}
                            </button>
                          );
                        })()}
                      </div>


                    </div>
                  </article>
                );
              })}
            </div>
          )}
        </section>

        <section className="how-section" id="how-it-works">
          <div className="section-heading centered">
            <div>
              <div className="eyebrow">THE HERO PROTOCOL</div>
              <h2>THREE STEPS. ONE GOOD DEED.</h2>
              <p>
                Great power includes helping people find their things.
              </p>
            </div>
          </div>

          <div className="steps-grid">
            <article className="step-card">
              <span className="step-number">01</span>
              <div className="step-icon">▣</div>
              <h3>OPEN A CASE</h3>
              <p>
                Sign in, describe the item, add a photo, and share
                a place if you choose.
              </p>
            </article>

            <article className="step-card">
              <span className="step-number">02</span>
              <div className="step-icon">⌕</div>
              <h3>SCAN THE BOARD</h3>
              <p>
                Search descriptions and compare photos and
                locations in community reports.
              </p>
            </article>

            <article className="step-card">
              <span className="step-number">03</span>
              <div className="step-icon">✦</div>
              <h3>MAKE THE MATCH</h3>
              <p>
                Help reunite a person with something they thought
                was gone for good.
              </p>
            </article>
          </div>
        </section>
      </main>

      <footer className="footer">
        <Brand />
        <p>With great community comes great responsibility.</p>
        <span>
          UNOFFICIAL FAN-INSPIRED DESIGN • NOT AFFILIATED WITH MARVEL
        </span>
      </footer>

      {/* AUTHENTICATION MODAL */}
      {modal === "auth" && (
        <div
          className="modal-backdrop"
          onMouseDown={(event) => {
            if (event.target === event.currentTarget && !authBusy) {
              setModal("");
              setError("");
            }
          }}
        >
          <section
            className="report-modal auth-modal"
            role="dialog"
            aria-modal="true"
            aria-labelledby="auth-title"
          >
            <div className="modal-heading">
              <div>
                <div className="eyebrow">AGENT ACCESS</div>
                <h2 id="auth-title">
                  {authMode === "login"
                    ? "WELCOME BACK, HERO"
                    : "JOIN THE INITIATIVE"}
                </h2>
                <p>
                  Your account protects ownership of your reports.
                </p>
              </div>

              <button
                type="button"
                className="close-button"
                disabled={authBusy}
                onClick={() => {
                  setModal("");
                  setError("");
                }}
                aria-label="Close"
              >
                ×
              </button>
            </div>

            {error && (
              <div className="modal-error" role="alert">
                {error}
              </div>
            )}

            <div className="form-type-toggle">
              <button
                type="button"
                className={authMode === "login" ? "selected" : ""}
                onClick={() => {
                  setAuthMode("login");
                  setError("");
                }}
              >
                SIGN IN
              </button>

              <button
                type="button"
                className={authMode === "register" ? "selected" : ""}
                onClick={() => {
                  setAuthMode("register");
                  setError("");
                }}
              >
                CREATE ACCOUNT
              </button>
            </div>

            <form className="report-form" onSubmit={handleAuth}>
              {authMode === "register" && (
                <label>
                  Full name *
                  <input
                    name="name"
                    type="text"
                    value={authForm.name}
                    onChange={updateAuth}
                    minLength={3}
                    maxLength={100}
                    required
                    autoComplete="name"
                    placeholder="Your full name"
                  />
                </label>
              )}

              <label>
                Email *
                <input
                  name="email"
                  type="email"
                  value={authForm.email}
                  onChange={updateAuth}
                  required
                  autoComplete="email"
                  placeholder="you@example.com"
                />
              </label>

              {authMode === "register" && (
                <label>
                  Phone number (optional)
                  <input
                    name="phone"
                    type="tel"
                    value={authForm.phone}
                    onChange={updateAuth}
                    maxLength={20}
                    autoComplete="tel"
                    placeholder="Your phone number"
                  />
                </label>
              )}

              {/* Only one password input in this form */}
              <label>
                Password *

                <input
                  name="password"
                  type="password"
                  value={authForm.password}
                  onChange={updateAuth}
                  minLength={authMode === "register" ? 8 : 1}
                  maxLength={100}
                  pattern={
                    authMode === "register"
                      ? "(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[^A-Za-z0-9]).{8,}"
                      : undefined
                  }
                  title={
                    authMode === "register"
                      ? "Use at least 8 characters, uppercase, lowercase, a number, and a special character."
                      : undefined
                  }
                  required
                  autoComplete={
                    authMode === "login"
                      ? "current-password"
                      : "new-password"
                  }
                  placeholder={
                    authMode === "register"
                      ? "Create a strong password"
                      : "Your password"
                  }
                />

                {authMode === "register" && (
                  <div className="password-strength">
                    <div className="password-strength-heading">
                      <span>Password strength</span>

                      <strong
                        className={
                          passwordStrength === 5
                            ? "strength-strong"
                            : passwordStrength >= 3
                              ? "strength-average"
                              : "strength-weak"
                        }
                      >
                        {passwordStrength === 5
                          ? "STRONG"
                          : passwordStrength >= 3
                            ? "AVERAGE"
                            : "WEAK"}
                      </strong>
                    </div>

                    <div className="password-strength-track">
                      <div
                        className={`password-strength-fill ${passwordStrength === 5
                          ? "strength-strong"
                          : passwordStrength >= 3
                            ? "strength-average"
                            : "strength-weak"
                          }`}
                        style={{
                          width: `${passwordStrength * 20}%`,
                        }}
                      />
                    </div>

                    <ul className="password-requirements">
                      {passwordChecks.map((check) => (
                        <li
                          key={check.label}
                          className={
                            check.valid ? "requirement-valid" : ""
                          }
                        >
                          <span>
                            {check.valid ? "✓" : "○"}
                          </span>
                          {check.label}
                        </li>
                      ))}
                    </ul>
                  </div>
                )}
              </label>

              <button
                type="submit"
                className="button button-primary submit-button"
                disabled={
                  authBusy ||
                  (authMode === "register" && !strongPassword)
                }
              >
                {authBusy
                  ? "AUTHENTICATING..."
                  : authMode === "login"
                    ? "SIGN IN →"
                    : "CREATE ACCOUNT →"}
              </button>

              <p className="privacy-note">
                Use a unique password. Your password should be
                securely hashed by the backend before it is stored.
              </p>
            </form>
          </section>
        </div>
      )}

      {/* REPORT ITEM MODAL */}
      {modal === "report" && (
        <div
          className="modal-backdrop"
          onMouseDown={(event) => {
            if (
              event.target === event.currentTarget &&
              !submitting &&
              !uploading &&
              !aiAnalyzing
            ) {
              setModal("");
              setError("");
            }
          }}
        >
          <section
            className="report-modal"
            role="dialog"
            aria-modal="true"
            aria-labelledby="report-title"
          >
            <div className="modal-heading">
              <div>
                <div className="eyebrow">NEW CASE FILE</div>
                <h2 id="report-title">
                  {reportType === "lost"
                    ? "REPORT A LOST ITEM"
                    : "REPORT A FOUND ITEM"}
                </h2>
                <p>
                  Be specific. Details help people recognize the item.
                </p>
              </div>

              <button
                type="button"
                className="close-button"
                aria-label="Close form"
                disabled={submitting || uploading || aiAnalyzing}
                onClick={() => {
                  setModal("");
                  setError("");
                }}
              >
                ×
              </button>
            </div>

            {error && (
              <div className="modal-error" role="alert">
                {error}
              </div>
            )}

            <div className="form-type-toggle">
              <button
                type="button"
                className={reportType === "lost" ? "selected" : ""}
                onClick={() => {
                  setReportType("lost");
                  setError("");
                }}
              >
                I LOST SOMETHING
              </button>

              <button
                type="button"
                className={reportType === "found" ? "selected" : ""}
                onClick={() => {
                  setReportType("found");
                  setError("");
                }}
              >
                I FOUND SOMETHING
              </button>
            </div>

            <form className="report-form" onSubmit={handleSubmit}>
              <label>
                Item name *
                <input
                  name="title"
                  value={form.title}
                  onChange={updateReport}
                  minLength={3}
                  maxLength={150}
                  required
                  placeholder="e.g. Blue backpack"
                />
              </label>

              <label>
                Category *
                <select
                  name="categoryId"
                  value={form.categoryId}
                  onChange={updateReport}
                  required
                >
                  <option value="">Choose a category</option>
                  {categories.map((category) => (
                    <option key={category.id} value={category.id}>
                      {category.name}
                    </option>
                  ))}
                </select>

                {categories.length === 0 && (
                  <small className="field-help">
                    Categories could not be loaded. Refresh the
                    community board and try again.
                  </small>
                )}
              </label>

              <label>
                Description *
                <textarea
                  name="description"
                  value={form.description}
                  onChange={updateReport}
                  minLength={10}
                  maxLength={2000}
                  required
                  placeholder="Color, brand, marks, contents (avoid sharing sensitive details)..."
                />
              </label>

              <label>
                Location or landmark *
                <input
                  name="location"
                  value={form.location}
                  onChange={updateReport}
                  minLength={3}
                  maxLength={200}
                  required
                  placeholder="e.g. Library, 2nd floor"
                />
              </label>

              <div className="location-action">
                <button
                  type="button"
                  className="button button-outline"
                  onClick={getCurrentLocation}
                >
                  ⌖ USE MY CURRENT LOCATION
                </button>
                <small>
                  Optional. Your browser asks permission first.
                </small>
              </div>

              {geoMessage && (
                <p className="geo-message">{geoMessage}</p>
              )}

              <label>
                Date {reportType === "lost" ? "lost" : "found"} *
                <input
                  name="date"
                  type="date"
                  value={form.date}
                  onChange={updateReport}
                  max={TODAY}
                  required
                />
              </label>

              <label>
                Photo (optional)
                <input
                  type="file"
                  accept="image/jpeg,image/png,image/webp"
                  onChange={chooseImage}
                  disabled={uploading || submitting || aiAnalyzing}
                />
                <small className="field-help">
                  JPEG, PNG or WebP, maximum 5 MB. AI checks whether the photo shows a lost-and-found item and suggests report details.
                </small>
              </label>

              {aiAnalyzing && (
                <p className="field-help" role="status">
                  ✨ AI is analyzing the photo. Please wait…
                </p>
              )}

              {aiAnalysis && (
                <div
                  className={aiAnalysis.suitable ? "notice" : "modal-error"}
                  role={aiAnalysis.suitable ? "status" : "alert"}
                >
                  <strong>
                    {aiAnalysis.suitable ? "AI item suggestion" : "Photo not suitable"}
                  </strong>
                  <p>{aiAnalysis.message || (aiAnalysis.suitable
                    ? "Review the suggested details before submitting."
                    : "Please choose a clear photo of a lost-and-found item.")}</p>
                  {aiAnalysis.suitable && (
                    <small>
                      Suggested: {aiAnalysis.title} · {aiAnalysis.category} · Confidence {Math.round((Number(aiAnalysis.confidence) || 0) * 100)}%
                    </small>
                  )}
                </div>
              )}

              {form.imagePreview && (
                <div className="image-preview-wrap">
                  <img
                    src={form.imagePreview}
                    alt="Selected item preview"
                  />

                  <button
                    type="button"
                    className="button button-outline"
                    disabled={submitting || uploading || aiAnalyzing}
                    onClick={() => {
                      URL.revokeObjectURL(form.imagePreview);

                      setForm((prev) => ({
                        ...prev,
                        imageFile: null,
                        imageUrl: "",
                        imagePreview: "",
                      }));
                      setAiAnalysis(null);
                      setError("");
                    }}
                  >
                    REMOVE PHOTO
                  </button>
                </div>
              )}

              <p className="privacy-note">
                Your report is linked to your signed-in account.
                Do not upload IDs, payment cards, or other sensitive
                personal documents.
              </p>

              <button
                type="submit"
                className="button button-primary submit-button"
                disabled={
                  submitting ||
                  uploading ||
                  aiAnalyzing ||
                  !form.categoryId
                }
              >
                {submitting
                  ? "SUBMITTING CASE..."
                  : aiAnalyzing
                    ? "ANALYZING PHOTO..."
                    : uploading
                      ? "UPLOADING PHOTO..."
                      : "SUBMIT CASE →"}
              </button>
            </form>
          </section>
        </div>
      )}
    </div>
  );
}
