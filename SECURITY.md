## Security Policy

BetterModel is a server-side 3D model engine that operates in an isolated environment without direct connections to external clients. As such, the risk of traditional security vulnerabilities is minimal.

#### Key Points

- 🔒 **No Client Connection**  
  BetterModel does not expose any network interface or accept input from external clients.

- 📦 **No Data Leakage**  
  Animation and bone data are handled on the server and are never sent to the client directly. Only processed vector packets are transmitted, which do not include raw model data.

- 🧱 **Model Privacy**  
  Your model files are converted into a Minecraft-compatible resource pack. During this conversion process, most of the original model information is stripped or transformed, minimizing the risk of leakage.
