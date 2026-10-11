const tokenInput = document.getElementById("token");
const status = document.getElementById("status");

chrome.storage.local.get("token").then(({ token }) => {
  tokenInput.value = token || "";
});

document.getElementById("save").addEventListener("click", async () => {
  const token = tokenInput.value.trim();
  if (token.length < 32) {
    status.textContent = "Enter a bridge token with at least 32 characters.";
    return;
  }
  await chrome.storage.local.set({ token });
  status.textContent = "Token saved.";
});
