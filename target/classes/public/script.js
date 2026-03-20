let currentUser = null;

async function fetchState() {
  try {
    const response = await fetch("/state");
    const data = await response.json();

    const activeUsersList = document.getElementById("activeUsers");
    const waitingUsersList = document.getElementById("waitingUsers");
    const fileStatus = document.getElementById("fileStatus");
    const fileContent = document.getElementById("fileContent");
    const fileTableBody = document.getElementById("fileTableBody");

    activeUsersList.innerHTML = "";
    waitingUsersList.innerHTML = "";
    fileTableBody.innerHTML = "";

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

    data.files.forEach(file => {
      const row = document.createElement("tr");

      row.innerHTML = `
        <td>${file.name}</td>
        <td>${file.status}</td>
        <td>
          <button class="action-btn" onclick="performFileAction('${file.name}', 'read')">Read</button>
          <button class="action-btn" onclick="performFileAction('${file.name}', 'write')">Write</button>
        </td>
      `;

      fileTableBody.appendChild(row);
    });
  } catch (error) {
    console.error("Error fetching state:", error);
  }
}

async function performFileAction(fileName, action) {
  const actionMessage = document.getElementById("actionMessage");

  if (!currentUser) {
    actionMessage.textContent = "You must log in first.";
    return;
  }

  try {
    const response = await fetch("/file-action", {
      method: "POST",
      headers: {
        "Content-Type": "application/json"
      },
      body: JSON.stringify({
        id: currentUser.id,
        username: currentUser.username,
        fileName: fileName,
        action: action
      })
    });

    const result = await response.text();
    actionMessage.textContent = result;
    fetchState();
  } catch (error) {
    actionMessage.textContent = "File action failed.";
    console.error(error);
  }
}

window.addEventListener("DOMContentLoaded", () => {
  const loginForm = document.getElementById("loginForm");
  const logoutBtn = document.getElementById("logoutBtn");
  const loginSection = document.getElementById("loginSection");
  const dashboardSection = document.getElementById("dashboardSection");
  const loginMessage = document.getElementById("loginMessage");
  const welcomeText = document.getElementById("welcomeText");

  loginForm.addEventListener("submit", async (e) => {
    e.preventDefault();

    const id = parseInt(document.getElementById("userId").value);
    const username = document.getElementById("username").value;

    try {
      const response = await fetch("/login", {
        method: "POST",
        headers: {
          "Content-Type": "application/json"
        },
        body: JSON.stringify({ id, username })
      });

      const result = await response.text();

      if (response.ok) {
        currentUser = { id, username };
        loginMessage.textContent = result;
        welcomeText.textContent = `Welcome, ${username}`;
        loginSection.style.display = "none";
        dashboardSection.style.display = "block";
        fetchState();
      } else {
        loginMessage.textContent = result;
      }
    } catch (error) {
      loginMessage.textContent = "Login failed.";
      console.error(error);
    }
  });

  logoutBtn.addEventListener("click", async () => {
    if (!currentUser) return;

    await fetch("/logout", {
      method: "POST",
      headers: {
        "Content-Type": "application/json"
      },
      body: JSON.stringify(currentUser)
    });

    currentUser = null;
    loginSection.style.display = "block";
    dashboardSection.style.display = "none";
    loginMessage.textContent = "Logged out.";
  });

  setInterval(fetchState, 2000);
});