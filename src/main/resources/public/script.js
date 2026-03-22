let currentUser = null;
let selectedFileName = null;
let waitingPollInterval = null;

async function pollUserStatus() {
  if (!currentUser) return;

  try {
    const response = await fetch("/user-status", {
      method: "POST",
      headers: {
        "Content-Type": "application/json"
      },
      body: JSON.stringify(currentUser)
    });

    const result = await response.text();

    if (result === "ACTIVE") {
      clearInterval(waitingPollInterval);
      waitingPollInterval = null;

      document.getElementById("waitingSection").style.display = "none";
      document.getElementById("dashboardSection").style.display = "block";
      document.getElementById("welcomeText").textContent = `Welcome, ${currentUser.username}`;
      fetchState();
      return;
    }

    if (result.startsWith("WAITING:")) {
      const position = result.split(":")[1];
      document.getElementById("waitingPositionText").textContent =
        `You are number ${position} in the queue waiting to login.`;
    }
  } catch (error) {
    console.error("Error polling user status:", error);
  }
}

async function fetchState() {
  try {
    const response = await fetch("/state");
    const data = await response.json();

    const activeUsersList = document.getElementById("activeUsers");
    const waitingUsersList = document.getElementById("waitingUsers");
    const fileStatus = document.getElementById("fileStatus");
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

    data.files.forEach(file => {
      const row = document.createElement("tr");

      row.innerHTML = `
        <td>${file.name}</td>
        <td>${file.status}</td>
        <td>
          <button class="action-btn" onclick="performRead('${file.name}')">Read</button>
          <button class="action-btn" onclick="openWriteEditor('${file.name}')">Write</button>
        </td>
      `;

      fileTableBody.appendChild(row);
    });
  } catch (error) {
    console.error("Error fetching state:", error);
  }
}
async function performRead(fileName) {
  const actionMessage = document.getElementById("actionMessage");

  if (!currentUser) {
    actionMessage.textContent = "You must log in first.";
    return;
  }

  try {
    const response = await fetch("/request-read", {
      method: "POST",
      headers: {
        "Content-Type": "application/json"
      },
      body: JSON.stringify({
        id: currentUser.id,
        username: currentUser.username
      })
    });

    const result = await response.text();

    if (result === "READ_GRANTED") {
      const stateResponse = await fetch("/state");
      const data = await stateResponse.json();

      document.getElementById("readViewerTitle").textContent = `Read File: ${fileName}`;
      document.getElementById("readViewerContent").textContent = data.fileContent;
      document.getElementById("readViewerSection").style.display = "block";
      document.getElementById("actionMessage").textContent = "Read access granted.";
      fetchState();
      return;
    }

    actionMessage.textContent = result;
  } catch (error) {
    actionMessage.textContent = "Read request failed.";
    console.error(error);
  }
}

async function openWriteEditor(fileName) {
  if (!currentUser) {
    document.getElementById("actionMessage").textContent = "You must log in first.";
    return;
  }

  try {
    const response = await fetch("/request-write", {
      method: "POST",
      headers: {
        "Content-Type": "application/json"
      },
      body: JSON.stringify({
        id: currentUser.id,
        username: currentUser.username
      })
    });

    const result = await response.text();

    if (result === "WRITE_GRANTED") {
      const stateResponse = await fetch("/state");
      const data = await stateResponse.json();

      selectedFileName = fileName;
      document.getElementById("editorTitle").textContent = `Edit File: ${fileName}`;
      document.getElementById("fileEditor").value = data.fileContent;
      document.getElementById("editorMessage").textContent = "";
      document.getElementById("actionMessage").textContent = "Write access granted.";
      document.getElementById("editorSection").style.display = "block";
      fetchState();
      return;
    }

    if (result.startsWith("WRITE_WAITING:")) {
      const position = result.split(":")[1];
      document.getElementById("actionMessage").textContent =
        `Write access unavailable. You are number ${position} in the write queue.`;
      fetchState();
      return;
    }

    document.getElementById("actionMessage").textContent = result;
  } catch (error) {
    document.getElementById("actionMessage").textContent = "Failed to request write access.";
    console.error(error);
  }
}

async function saveFileChanges() {
  const editorMessage = document.getElementById("editorMessage");

  if (!currentUser || !selectedFileName) {
    editorMessage.textContent = "No file selected.";
    return;
  }

  const editedContent = document.getElementById("fileEditor").value;

  try {
    const response = await fetch("/file-action", {
      method: "POST",
      headers: {
        "Content-Type": "application/json"
      },
      body: JSON.stringify({
        id: currentUser.id,
        username: currentUser.username,
        fileName: selectedFileName,
        action: "write",
        content: editedContent
      })
    });

    const result = await response.text();
    editorMessage.textContent = "Changes saved.";
    document.getElementById("actionMessage").textContent = result;

    // keep editor open
    fetchState();
  } catch (error) {
    editorMessage.textContent = "Save failed.";
    console.error(error);
  }
}

window.addEventListener("DOMContentLoaded", () => {
  const loginForm = document.getElementById("loginForm");
  const logoutBtn = document.getElementById("logoutBtn");
  const saveFileBtn = document.getElementById("saveFileBtn");
  const exitEditBtn = document.getElementById("exitEditBtn");

  const loginSection = document.getElementById("loginSection");
  const waitingSection = document.getElementById("waitingSection");
  const dashboardSection = document.getElementById("dashboardSection");
  const loginMessage = document.getElementById("loginMessage");
  const welcomeText = document.getElementById("welcomeText");
  const closeReadBtn = document.getElementById("closeReadBtn");

  closeReadBtn.addEventListener("click", async () => {
    if (currentUser) {
      await fetch("/release-read", {
        method: "POST",
        headers: {
          "Content-Type": "application/json"
        },
        body: JSON.stringify(currentUser)
      });
    }

    document.getElementById("readViewerSection").style.display = "none";
    fetchState();
  });

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

      if (result === "LOGIN_SUCCESS") {
        currentUser = { id, username };
        loginMessage.textContent = "";
        welcomeText.textContent = `Welcome, ${username}`;
        loginSection.style.display = "none";
        waitingSection.style.display = "none";
        dashboardSection.style.display = "block";
        fetchState();
      } else if (result.startsWith("WAITING:")) {
        currentUser = { id, username };
        const position = result.split(":")[1];

        loginMessage.textContent = "";
        loginSection.style.display = "none";
        dashboardSection.style.display = "none";
        waitingSection.style.display = "block";
        document.getElementById("waitingPositionText").textContent =
          `You are number ${position} in the queue waiting to login.`;

        if (waitingPollInterval) {
          clearInterval(waitingPollInterval);
        }
        waitingPollInterval = setInterval(pollUserStatus, 2000);
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

    await fetch("/release-write", {
      method: "POST",
      headers: {
        "Content-Type": "application/json"
      },
      body: JSON.stringify(currentUser)
    });
    
    await fetch("/logout", {
      method: "POST",
      headers: {
        "Content-Type": "application/json"
      },
      body: JSON.stringify(currentUser)
    });

    if (waitingPollInterval) {
      clearInterval(waitingPollInterval);
      waitingPollInterval = null;
    }

    currentUser = null;
    selectedFileName = null;

    document.getElementById("editorSection").style.display = "none";
    loginSection.style.display = "block";
    waitingSection.style.display = "none";
    dashboardSection.style.display = "none";
    loginMessage.textContent = "Logged out.";
  });

  saveFileBtn.addEventListener("click", saveFileChanges);

  exitEditBtn.addEventListener("click", async () => {
    if (currentUser) {
      await fetch("/release-write", {
        method: "POST",
        headers: {
          "Content-Type": "application/json"
        },
        body: JSON.stringify(currentUser)
      });
    }

    selectedFileName = null;
    document.getElementById("editorSection").style.display = "none";
    document.getElementById("editorMessage").textContent = "";
    fetchState();
  });

  setInterval(fetchState, 2000);
});