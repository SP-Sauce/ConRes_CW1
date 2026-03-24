# ConRes Demo Source Code Reference
---
# Video of Demo Source Code
Video Link: 

https://youtu.be/-Q49XQPdWWA
  
---

This document identifies the key source code locations that demonstrate the three concurrency concepts required in the demo marking criteria:

- **Thread Creation**
- **Semaphores**
- **Locks**

These references can be used in place of showing every code file during the video. They point directly to the classes and line ranges where each concept is implemented in the submitted Java source code.

---

## 1. Thread Creation

### Primary files
- `UserSession.java`
- `SessionManager.java`

### Source code locations

#### `UserSession.java`
- **Line 6** – `UserSession` is defined as `extends Thread`
- **Lines 12–16** – constructor assigns the user and thread name
- **Lines 41–43** – `terminate()` stops the session thread safely
- **Lines 47–59** – `run()` contains the thread execution logic

#### `SessionManager.java`
- **Lines 39–41** – a new `UserSession` is created and started when login succeeds
- **Lines 86–89** – a new `UserSession` is created and started for the next queued user

### Explanation
Thread creation in the system is mainly implemented through the `UserSession` class. Each authenticated user is represented by a separate thread, allowing the system to model multiple active users concurrently. In `SessionManager`, a `UserSession` object is instantiated and started when a permit is available, and another session thread is started when a waiting user is promoted.

---

## 2. Semaphores

### Primary file
- `SessionManager.java`

### Source code locations
- **Line 4** – imports `Semaphore`
- **Line 8** – semaphore declared as `loginSemaphore`
- **Lines 13–18** – semaphore initialised in the constructor
- **Line 14** – `new Semaphore(maxConcurrentUsers, true)` creates a fair counting semaphore
- **Line 36** – `tryAcquire()` attempts to obtain a permit for login
- **Lines 47–51** – users are placed into a waiting queue if no permit is available
- **Line 72** – `release()` returns the permit on logout
- **Lines 75–96** – the next waiting user is promoted after a permit is released

### Explanation
A fair counting semaphore is used in `SessionManager` to enforce the maximum number of concurrent users. When a user attempts to log in, the system calls `tryAcquire()` on the semaphore. If a permit is available, login succeeds; if not, the user is placed into the waiting queue. When a user logs out, the permit is released, allowing another waiting user to be admitted.

This directly supports the scenario requirement that only **N concurrent users** may access the system at once.

---

## 3. Locks

### Primary file
- `FileAccessManager.java`

### Source code locations

#### Lock declaration and initialisation
- **Line 9** – imports `ReentrantReadWriteLock`
- **Line 14** – read/write lock declared
- **Lines 24–32** – lock initialised in the constructor
- **Line 26** – `new ReentrantReadWriteLock(true)` creates a fair read/write lock

#### Read locking
- **Lines 124–145** – `readFile()` acquires the read lock, performs the read, and releases the lock in `finally`
- **Line 125** – `lock.readLock().lock()`
- **Line 144** – `lock.readLock().unlock()`

#### Write locking
- **Lines 148–190** – `writeFile()` controls exclusive writing
- **Line 156** – `lock.writeLock().tryLock(10, TimeUnit.SECONDS)` attempts to acquire the write lock with timeout
- **Lines 166–170** – protected file write operation
- **Lines 183–188** – writer state cleared and write lock released in `finally`

#### Read access coordination
- **Lines 193–205** – `requestReadAccess()` grants tracked read access
- **Lines 207–218** – `releaseReadAccess()` removes tracked read access and unlocks

### Explanation
Locks are implemented using `ReentrantReadWriteLock` in `FileAccessManager` to protect the shared file resource. The read lock allows multiple users to read the file concurrently, while the write lock ensures that only one user can modify the file at a time. The write operation uses `tryLock` with a timeout, which improves robustness by avoiding indefinite blocking.

This is the main mechanism used to prevent unsafe simultaneous access to the shared file and to enforce the readers-writer behaviour required by the scenario.


- **Semaphores:** `SessionManager` uses a fair counting semaphore to control the maximum number of active users.
- **Locks:** `FileAccessManager` uses a fair `ReentrantReadWriteLock` to coordinate safe concurrent file access.

