Orchestrator infra for managing containers on a remote server as you would with VMs.

## Run with Docker Compose (remote-friendly)

This repo includes:

- `compose.yaml`: **local dev** (frontend + backend + postgres, hot reload)
- `compose.prod.yaml`: **production-style** (frontend + backend + postgres, key-based SSH via Docker secret)

### Production-style (recommended): deploy with a dedicated SSH key

Your backend runs `ssh` non-interactively to control a remote machine. Best practice is to use a **dedicated deploy key** and mount it into the backend container as a **Docker secret**.

#### 1) Generate a dedicated deploy key

Run this on any machine you trust (your laptop or the deploy host):

```bash
ssh-keygen -t ed25519 -f orchestrator_deploy_key -C "core-orchestrator deploy"
```

This creates:
- `orchestrator_deploy_key` (private key) — keep secret
- `orchestrator_deploy_key.pub` (public key) — install on the remote machine you will SSH into

#### 2) Install the public key on the remote machine you will SSH into

On the **remote machine you want to control** (as the target SSH user), add the public key to `~/.ssh/authorized_keys`:

```bash
mkdir -p ~/.ssh
chmod 700 ~/.ssh
cat orchestrator_deploy_key.pub >> ~/.ssh/authorized_keys
chmod 600 ~/.ssh/authorized_keys
```

#### 3) Put the private key on the deploy host (the machine running Docker)

On the **deploy host** (the machine where you run `docker compose` for this repo):

```bash
mkdir -p secrets
cp /path/to/orchestrator_deploy_key ./secrets/id_rsa
chmod 600 ./secrets/id_rsa
```

Notes:
- The path `./secrets/id_rsa` is required by `compose.prod.yaml`.
- `secrets/` is gitignored; do not commit keys.

#### 4) Start the stack

From the repo root:

```bash
SSH_HOST="<remote-hostname-or-ip>" \
SSH_USERNAME="<remote-ssh-username>" \
STORAGE_BASE_PATH="<remote-path-on-that-machine>" \
docker compose -f compose.prod.yaml up -d --build
```

#### Stop the stack

From the repo root:

```bash
docker compose -f compose.prod.yaml down
```

If you also want to delete the Postgres volume data on the deploy host (full reset):

```bash
docker compose -f compose.prod.yaml down -v
```

#### Environment variables (what they mean)

- `SSH_HOST`: **Hostname/IP of the remote machine to control** (the SSH target).
  - Example: `10.0.0.15` or `lab-server.example.com`
- `SSH_USERNAME`: **SSH username on the remote machine** (the user that owns `~/.ssh/authorized_keys` you updated).
  - Example: `ubuntu`, `admin`, `labuser`
- `VITE_SSH_HOST`: **Hostname/IP shown to students in the UI** for the `ssh ...` command.
  - Defaults to `SSH_HOST` if not set.
- `VITE_SSH_USER`: **Username shown to students in the UI** for the `ssh ...` command.
  - Default: `student`
- `SSH_KEY_PATH`: **Path to the private key inside the backend container**.
  - You usually do not set this manually in prod; `compose.prod.yaml` sets it to `/run/secrets/ssh_private_key`.
- `STORAGE_BASE_PATH`: **Base directory on the remote machine** where per-student data folders are created.
  - Example: `/srv/lab-data` or `/home/<user>/lab-data`
- `STORAGE_CONTAINER_PATH`: **Path inside the student containers** where the storage is mounted.
  - Default: `/home/student`

#### Common commands

```bash
docker compose -f compose.prod.yaml logs -f backend frontend postgres
docker compose -f compose.prod.yaml down
```
