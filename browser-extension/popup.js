const status = document.getElementById("status");

document.getElementById("send").addEventListener("click", async () => {
  status.textContent = "";
  try {
    const { token } = await chrome.storage.local.get("token");
    if (!token) {
      status.textContent = "Set the bridge token in this extension's options first.";
      return;
    }
    const [tab] = await chrome.tabs.query({ active: true, currentWindow: true });
    let url = tab?.url ?? "";
    const parsed = new URL(url);
    const host = parsed.hostname.toLowerCase();
    const youtubeVideoId = parsed.searchParams.get("v") ||
      parsed.pathname.match(/^\/(?:shorts|embed|live)\/([A-Za-z0-9_-]{6,})/)?.[1];
    const videoUrl = host === "youtu.be"
      ? /^\/[A-Za-z0-9_-]{6,}\/?$/.test(parsed.pathname)
      : ["youtube.com", "www.youtube.com", "m.youtube.com", "music.youtube.com"]
          .includes(host) && /^[A-Za-z0-9_-]{6,}$/.test(youtubeVideoId || "");
    if (
      !videoUrl ||
      (parsed.searchParams.has("list") && !parsed.searchParams.has("v"))
    ) {
      status.textContent = "Open a single YouTube video, not a playlist.";
      return;
    }
    parsed.searchParams.delete("list");
    url = parsed.toString();
    const response = await fetch("http://127.0.0.1:43721/enqueue", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "Authorization": `Bearer ${token}`
      },
      body: JSON.stringify({ url })
    });
    const result = await response.json();
    if (!response.ok) throw new Error(result.message || `Request failed (${response.status}).`);
    status.textContent = result.message;
  } catch (error) {
    status.textContent = error.message || "Could not connect to Seal Desktop.";
  }
});
