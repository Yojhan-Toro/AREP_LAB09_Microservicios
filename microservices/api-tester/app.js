const USER_API = "http://localhost:8081";
const POST_API = "http://localhost:8082";
const STREAM_API = "http://localhost:8083";

const AUTH0_DOMAIN = "dev-iz60ngd4p8tr0cql.us.auth0.com";
const AUTH0_CLIENT_ID = "gd7gVXghlBkxnCK7bimBSlEveGMz2vmF";
const AUTH0_AUDIENCE = "https://twitter-monolith-api";
const REDIRECT_URI = "http://localhost:5050/index.html";

let accessToken = null;
let currentUser = null;
let currentPage = 0;
const PAGE_SIZE = 10;

globalThis.onload = async () => {
  const params = new URLSearchParams(globalThis.location.search);
  if (params.has("code")) {
    await handleCallback(params.get("code"));
    globalThis.history.replaceState({}, document.title, globalThis.location.pathname);
  } else {
    const saved = sessionStorage.getItem("access_token");
    if (saved) {
      accessToken = saved;
      await loadUserProfile();
    }
  }

  renderAuth();
  await checkServices();
  await loadStream();
};

function base64URLEncode(buffer) {
  let binary = "";
  const bytes = new Uint8Array(buffer);
  for (const byte of bytes) {
    binary += String.fromCodePoint(byte);
  }

  return btoa(binary)
    .replaceAll("+", "-")
    .replaceAll("/", "_")
    .replaceAll("=", "");
}

async function generateCodeVerifier() {
  const array = new Uint8Array(32);
  crypto.getRandomValues(array);
  return base64URLEncode(array);
}

async function generateCodeChallenge(verifier) {
  const encoded = new TextEncoder().encode(verifier);
  const hash = await crypto.subtle.digest("SHA-256", encoded);
  return base64URLEncode(hash);
}

async function login() {
  const verifier = await generateCodeVerifier();
  const challenge = await generateCodeChallenge(verifier);
  sessionStorage.setItem("pkce_verifier", verifier);

  const url = `https://${AUTH0_DOMAIN}/authorize?` + new URLSearchParams({
    response_type: "code",
    client_id: AUTH0_CLIENT_ID,
    redirect_uri: REDIRECT_URI,
    scope: "openid profile email read:posts write:posts read:profile",
    audience: AUTH0_AUDIENCE,
    code_challenge: challenge,
    code_challenge_method: "S256"
  });

  globalThis.location.href = url;
}

async function handleCallback(code) {
  const verifier = sessionStorage.getItem("pkce_verifier");
  const res = await fetch(`https://${AUTH0_DOMAIN}/oauth/token`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      grant_type: "authorization_code",
      client_id: AUTH0_CLIENT_ID,
      code,
      redirect_uri: REDIRECT_URI,
      code_verifier: verifier
    })
  });

  const data = await res.json();
  if (data.access_token) {
    accessToken = data.access_token;
    sessionStorage.setItem("access_token", accessToken);
    await loadUserProfile();
    showToast("Sesion iniciada");
  } else {
    showToast("Error al iniciar sesion", true);
  }
}

function logout() {
  accessToken = null;
  currentUser = null;
  sessionStorage.removeItem("access_token");
  renderAuth();

  const url = `https://${AUTH0_DOMAIN}/v2/logout?` + new URLSearchParams({
    client_id: AUTH0_CLIENT_ID,
    returnTo: REDIRECT_URI
  });
  globalThis.location.href = url;
}

async function apiFetch(url, options = {}, requiresAuth = false) {
  const headers = { "Content-Type": "application/json", ...options.headers };
  if (requiresAuth) {
    if (!accessToken) throw new Error("Debes iniciar sesion");
    headers.Authorization = `Bearer ${accessToken}`;
  }

  const res = await fetch(url, { ...options, headers });
  if (!res.ok) {
    const err = await res.json().catch(() => ({}));
    throw new Error(err.message || `HTTP ${res.status}`);
  }
  return res.json();
}

async function checkServices() {
  await Promise.all([
    probeService(`${USER_API}/v3/api-docs`, "status-user"),
    probeService(`${POST_API}/v3/api-docs`, "status-post"),
    probeService(`${STREAM_API}/v3/api-docs`, "status-stream")
  ]);
}

async function probeService(url, statusId) {
  const statusEl = document.getElementById(statusId);
  try {
    const res = await fetch(url);
    if (res.ok) {
      statusEl.textContent = "UP";
      statusEl.className = "service-status up";
      return;
    }
    throw new Error("Down");
  } catch {
    statusEl.textContent = "DOWN";
    statusEl.className = "service-status down";
  }
}

async function loadUserProfile() {
  if (!accessToken) return;

  try {
    currentUser = await apiFetch(`${USER_API}/api/me`, {}, true);
    const myPosts = await apiFetch(`${USER_API}/api/me/posts`, {}, true);
    const totalPosts = Array.isArray(myPosts) ? myPosts.length : 0;
    document.getElementById("profileSummary").textContent =
      `@${currentUser.username} | ${currentUser.email || "sin email"} | posts: ${totalPosts}`;
  } catch (error) {
    showToast(`Error perfil: ${error.message}`, true);
    accessToken = null;
    currentUser = null;
    sessionStorage.removeItem("access_token");
  }
}

async function loadStream() {
  const postsList = document.getElementById("postsList");
  postsList.innerHTML = '<div class="spinner"></div>';

  try {
    const data = await apiFetch(`${STREAM_API}/api/stream?page=${currentPage}&size=${PAGE_SIZE}`);
    renderPosts(data.posts || []);
    renderPagination(data);
  } catch (error) {
    postsList.innerHTML = `<p class="empty">Error al cargar stream: ${error.message}</p>`;
  }
}

async function createPost() {
  const input = document.getElementById("postContent");
  const content = input.value.trim();
  if (!content) return;
  if (content.length > 140) {
    showToast("Maximo 140 caracteres", true);
    return;
  }

  const btn = document.getElementById("btnPost");
  btn.disabled = true;
  btn.textContent = "Publicando...";

  try {
    await apiFetch(`${POST_API}/api/posts`, {
      method: "POST",
      body: JSON.stringify({ content })
    }, true);

    input.value = "";
    updateCharCount();
    currentPage = 0;
    await loadStream();
    await loadUserProfile();
    showToast("Post publicado");
  } catch (error) {
    showToast(`No se pudo publicar: ${error.message}`, true);
  } finally {
    btn.disabled = false;
    btn.textContent = "Publicar";
  }
}

function renderAuth() {
  const loggedIn = !!currentUser;
  document.getElementById("authBanner").style.display = loggedIn ? "none" : "block";
  document.getElementById("composeBox").style.display = loggedIn ? "block" : "none";
  document.getElementById("myProfile").style.display = loggedIn ? "block" : "none";
  document.getElementById("btnLogin").style.display = loggedIn ? "none" : "inline-block";
  document.getElementById("btnLogout").style.display = loggedIn ? "inline-block" : "none";
  document.getElementById("navUsername").textContent = loggedIn ? `@${currentUser.username}` : "";
}

function renderPosts(posts) {
  const el = document.getElementById("postsList");
  if (!posts.length) {
    el.innerHTML = '<p class="empty">No hay posts aun.</p>';
    return;
  }

  el.innerHTML = posts.map((post) => `
    <article class="post-card">
      <div class="post-header">
        <div class="post-avatar">
          ${post.authorPicture
            ? `<img src="${post.authorPicture}" alt="${escapeHtml(post.authorUsername || "user")}" />`
            : escapeHtml((post.authorUsername || "U").charAt(0).toUpperCase())}
        </div>
        <div class="post-meta">
          <strong>@${escapeHtml(post.authorUsername || "desconocido")}</strong>
          <time>${formatDate(post.createdAt)}</time>
        </div>
      </div>
      <div class="post-content">${escapeHtml(post.content || "")}</div>
    </article>
  `).join("");
}

function renderPagination(data) {
  const controls = document.getElementById("paginationControls");
  if (!data || data.totalPages <= 1) {
    controls.style.display = "none";
    return;
  }

  controls.style.display = "flex";
  document.getElementById("pageInfo").textContent = `Pagina ${data.page + 1} de ${data.totalPages}`;
  document.getElementById("btnPrev").disabled = data.page === 0;
  document.getElementById("btnNext").disabled = !data.hasNext;
}

function changePage(delta) {
  currentPage = Math.max(0, currentPage + delta);
  loadStream();
}

function updateCharCount() {
  const el = document.getElementById("charCount");
  const len = document.getElementById("postContent").value.length;
  el.textContent = `${len} / 140`;

  let cssClass = "";
  if (len > 140) {
    cssClass = "error";
  } else if (len > 130) {
    cssClass = "warn";
  }

  el.className = cssClass;
}

function showToast(message, isError = false) {
  const toast = document.getElementById("toast");
  toast.textContent = message;
  toast.className = isError ? "error show" : "show";
  setTimeout(() => {
    toast.className = isError ? "error" : "";
  }, 2600);
}

function escapeHtml(text) {
  return String(text)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

function formatDate(iso) {
  if (!iso) return "sin fecha";
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) return "sin fecha";
  return date.toLocaleDateString("es-CO", {
    day: "2-digit",
    month: "short",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit"
  });
}