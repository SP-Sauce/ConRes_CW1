async function fetchState() {
  try {
    const response = await fetch("/state");
    const data = await response.json();

    const activeUsersList = document.getElementById("activeUsers");
    const waitingUsersList = document.getElementById("waitingUsers");
    const fileStatus = document.getElementById("fileStatus");
    const fileContent = document.getElementById("fileContent");

    if (!activeUsersList || !waitingUsersList || !fileStatus || !fileContent) {
      console.error("Dashboard elements not found");
      return;
    }

    activeUsersList.innerHTML = "";
    waitingUsersList.innerHTML = "";

    data.activeUsers.forEach(user => {
      const li = document.createElement("li");
      li.textContent = user;
      activeUsersList.appendChild(li);
    });

    data.waitingUsers.forEach(user => {
      const li = document.createElement("li");
      li.textContent = user;
      waitingUsersList.appendChild(li);
    });

    fileStatus.textContent = data.fileStatus;
    fileContent.textContent = data.fileContent;
  } catch (error) {
    console.error("Error fetching state:", error);
  }
}

window.addEventListener("DOMContentLoaded", () => {
  const loginForm = document.getElementById("loginForm");

  if (!loginForm) {
    console.error("loginForm not found");
    return;
  }

  loginForm.addEventListener("submit", async (e) => {
    e.preventDefault();

    const userId = document.getElementById("userId").value;
    const username = document.getElementById("username").value;
    const action = document.getElementById("action").value;
    const message = document.getElementById("message");

    try {
      const response = await fetch("/login", {
        method: "POST",
        headers: {
          "Content-Type": "application/json"
        },
        body: JSON.stringify({
          id: parseInt(userId),
          username: username,
          action: action
        })
      });

      const result = await response.text();
      message.textContent = result;
      fetchState();
    } catch (error) {
      message.textContent = "Login request failed.";
      console.error(error);
    }
  });

  fetchState();
  setInterval(fetchState, 2000);
});