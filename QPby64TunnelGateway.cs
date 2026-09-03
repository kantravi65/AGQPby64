using System;
using System.Diagnostics;
using System.Drawing;
using System.Drawing.Drawing2D;
using System.IO;
using System.Text.RegularExpressions;
using System.Threading;
using System.Windows.Forms;

namespace QPby64.TunnelGateway
{
    public class MainForm : Form
    {
        private Button btnToggle;
        private Button btnRefreshDevice;
        private Button btnClearLog;
        private Label lblStatus;
        private Label lblPublicUrl;
        private Label lblDevice;
        private Label lblLogTitle;
        private TextBox txtLog;
        private Panel panelHeader;
        private Panel cardStatus;
        private Panel cardUrl;
        private Panel panelLinks;
        private NotifyIcon notifyIcon;
        private ContextMenuStrip trayMenu;
        private System.Windows.Forms.Timer deviceTimer;

        private Process tunnelProcess;
        private string activeTunnelUrl = "";
        private bool isRunning = false;
        private string adbPath = "";
        private string cloudflaredPath = "";

        // Modern Slate Dark Palette
        private Color clrBg = Color.FromArgb(15, 23, 42);          // Slate 900
        private Color clrCard = Color.FromArgb(30, 41, 59);        // Slate 800
        private Color clrCardBorder = Color.FromArgb(51, 65, 85);  // Slate 700
        private Color clrAccent = Color.FromArgb(14, 165, 233);    // Sky 500
        private Color clrSuccess = Color.FromArgb(16, 185, 129);   // Emerald 500
        private Color clrWarning = Color.FromArgb(245, 158, 11);   // Amber 500
        private Color clrError = Color.FromArgb(239, 68, 68);      // Red 500
        private Color clrText = Color.FromArgb(248, 250, 252);     // Slate 50
        private Color clrSubtext = Color.FromArgb(148, 163, 184);  // Slate 400

        [STAThread]
        public static void Main()
        {
            Application.EnableVisualStyles();
            Application.SetCompatibleTextRenderingDefault(false);
            Application.Run(new MainForm());
        }

        public MainForm()
        {
            InitializeComponent();
            ResolveBinaries();
            CheckDevice();

            deviceTimer = new System.Windows.Forms.Timer();
            deviceTimer.Interval = 5000;
            deviceTimer.Tick += (s, e) => CheckDevice();
            deviceTimer.Start();
        }

        private void InitializeComponent()
        {
            this.Text = "QPby64 Internet Tunnel Gateway";
            this.Size = new Size(880, 720);
            this.MinimumSize = new Size(800, 640);
            this.StartPosition = FormStartPosition.CenterScreen;
            this.BackColor = clrBg;
            this.ForeColor = clrText;
            this.Font = new Font("Segoe UI", 9.5f, FontStyle.Regular);
            this.DoubleBuffered = true;

            // Header Panel
            panelHeader = new Panel
            {
                Dock = DockStyle.Top,
                Height = 80,
                BackColor = Color.FromArgb(10, 15, 30),
                Padding = new Padding(24, 14, 24, 14)
            };

            Label lblTitle = new Label
            {
                Text = "⚡ QPby64 Internet Tunnel Gateway",
                Font = new Font("Segoe UI", 15f, FontStyle.Bold),
                ForeColor = clrText,
                AutoSize = true,
                Location = new Point(20, 14)
            };

            Label lblSubtitle = new Label
            {
                Text = "High-speed encrypted Cloudflare relay • Auto-syncs live exam & proctor portals to phone",
                Font = new Font("Segoe UI", 8.5f),
                ForeColor = clrSubtext,
                AutoSize = true,
                Location = new Point(22, 46)
            };

            panelHeader.Controls.Add(lblTitle);
            panelHeader.Controls.Add(lblSubtitle);
            this.Controls.Add(panelHeader);

            // Card 1: Status & Device Card
            cardStatus = new Panel
            {
                BackColor = clrCard,
                Padding = new Padding(16)
            };

            Label lblDevTitle = new Label
            {
                Text = "CONNECTED ANDROID PHONE:",
                Font = new Font("Segoe UI", 8f, FontStyle.Bold),
                ForeColor = clrSubtext,
                Location = new Point(16, 14),
                AutoSize = true
            };
            cardStatus.Controls.Add(lblDevTitle);

            lblDevice = new Label
            {
                Text = "Detecting via ADB...",
                Font = new Font("Segoe UI", 10.5f, FontStyle.Bold),
                ForeColor = clrAccent,
                Location = new Point(16, 34),
                AutoSize = true
            };
            cardStatus.Controls.Add(lblDevice);

            btnRefreshDevice = new Button
            {
                Text = "🔄 Refresh",
                Font = new Font("Segoe UI", 8f),
                BackColor = Color.FromArgb(51, 65, 85),
                ForeColor = Color.White,
                FlatStyle = FlatStyle.Flat,
                Size = new Size(80, 24),
                Cursor = Cursors.Hand
            };
            btnRefreshDevice.FlatAppearance.BorderSize = 0;
            btnRefreshDevice.Click += (s, e) => CheckDevice();
            cardStatus.Controls.Add(btnRefreshDevice);

            Label lblStatTitle = new Label
            {
                Text = "GATEWAY STATUS:",
                Font = new Font("Segoe UI", 8f, FontStyle.Bold),
                ForeColor = clrSubtext,
                Location = new Point(16, 68),
                AutoSize = true
            };
            cardStatus.Controls.Add(lblStatTitle);

            lblStatus = new Label
            {
                Text = "🔴 Offline - Ready to Connect",
                Font = new Font("Segoe UI", 11f, FontStyle.Bold),
                ForeColor = clrError,
                Location = new Point(16, 86),
                AutoSize = true
            };
            cardStatus.Controls.Add(lblStatus);

            // Large, unmissable START/STOP button
            btnToggle = new Button
            {
                Text = "🚀 START TUNNEL",
                Font = new Font("Segoe UI", 11.5f, FontStyle.Bold),
                BackColor = clrSuccess,
                ForeColor = Color.White,
                FlatStyle = FlatStyle.Flat,
                Size = new Size(220, 58),
                Cursor = Cursors.Hand
            };
            btnToggle.FlatAppearance.BorderSize = 0;
            btnToggle.Click += BtnToggle_Click;
            cardStatus.Controls.Add(btnToggle);

            this.Controls.Add(cardStatus);

            // Card 2: Public Tunnel Active URL Card
            cardUrl = new Panel
            {
                BackColor = clrCard,
                Padding = new Padding(16)
            };

            Label lblUrlTitle = new Label
            {
                Text = "PUBLIC TUNNEL BASE URL (AUTO-SYNCED TO PHONE & CLIPBOARD):",
                Font = new Font("Segoe UI", 8f, FontStyle.Bold),
                ForeColor = clrSubtext,
                Location = new Point(16, 12),
                AutoSize = true
            };
            cardUrl.Controls.Add(lblUrlTitle);

            lblPublicUrl = new Label
            {
                Text = "Waiting for tunnel to start...",
                Font = new Font("Consolas", 12f, FontStyle.Bold),
                ForeColor = Color.FromArgb(203, 213, 225),
                Location = new Point(16, 34),
                AutoSize = true
            };
            cardUrl.Controls.Add(lblPublicUrl);
            this.Controls.Add(cardUrl);

            // Card 3: Portal Links Panel
            panelLinks = new Panel
            {
                BackColor = Color.Transparent
            };
            this.Controls.Add(panelLinks);

            // Card 4: Log Label & Clear Button
            lblLogTitle = new Label
            {
                Text = "LIVE GATEWAY ACTIVITY & EVENT LOG:",
                Font = new Font("Segoe UI", 8f, FontStyle.Bold),
                ForeColor = clrSubtext,
                AutoSize = true
            };
            this.Controls.Add(lblLogTitle);

            btnClearLog = new Button
            {
                Text = "Clear Log",
                Font = new Font("Segoe UI", 7.5f),
                BackColor = Color.FromArgb(51, 65, 85),
                ForeColor = Color.White,
                FlatStyle = FlatStyle.Flat,
                Size = new Size(70, 22),
                Cursor = Cursors.Hand
            };
            btnClearLog.FlatAppearance.BorderSize = 0;
            btnClearLog.Click += (s, e) => txtLog.Clear();
            this.Controls.Add(btnClearLog);

            // Activity Log Box
            txtLog = new TextBox
            {
                Multiline = true,
                ReadOnly = true,
                ScrollBars = ScrollBars.Vertical,
                BackColor = Color.FromArgb(8, 12, 24),
                ForeColor = Color.FromArgb(226, 232, 240),
                Font = new Font("Consolas", 9f),
                BorderStyle = BorderStyle.FixedSingle
            };
            this.Controls.Add(txtLog);

            // System Tray Menu & Icon
            trayMenu = new ContextMenuStrip();
            trayMenu.Items.Add("Open Gateway Window", null, (s, e) => RestoreWindow());
            trayMenu.Items.Add("Copy Candidate Portal URL", null, (s, e) => CopyPortalUrl("/livetest"));
            trayMenu.Items.Add(new ToolStripSeparator());
            trayMenu.Items.Add("Exit", null, (s, e) => ExitApp());

            notifyIcon = new NotifyIcon
            {
                Text = "QPby64 Tunnel Gateway",
                Icon = SystemIcons.Shield,
                ContextMenuStrip = trayMenu,
                Visible = true
            };
            notifyIcon.DoubleClick += (s, e) => RestoreWindow();

            this.FormClosing += MainForm_FormClosing;
            this.Resize += (s, e) => UpdateLayout();

            // Initial Layout Calculation
            UpdateLayout();
        }

        private void UpdateLayout()
        {
            int pad = 20;
            int availableWidth = this.ClientSize.Width - (pad * 2);
            if (availableWidth < 500) availableWidth = 500;

            // 1. Status & Device Card
            cardStatus.Location = new Point(pad, panelHeader.Bottom + 14);
            cardStatus.Size = new Size(availableWidth, 122);

            // Position Start/Stop Button on right side of cardStatus
            btnToggle.Location = new Point(cardStatus.ClientSize.Width - btnToggle.Width - 18, 32);
            btnRefreshDevice.Location = new Point(btnToggle.Left - 95, 32);

            // 2. Public URL Card
            cardUrl.Location = new Point(pad, cardStatus.Bottom + 12);
            cardUrl.Size = new Size(availableWidth, 72);

            // 3. Portal Links Panel
            panelLinks.Location = new Point(pad, cardUrl.Bottom + 12);
            panelLinks.Size = new Size(availableWidth, 190);
            BuildPortalLinks(panelLinks);

            // 4. Log Section
            lblLogTitle.Location = new Point(pad, panelLinks.Bottom + 10);
            btnClearLog.Location = new Point(pad + availableWidth - btnClearLog.Width, lblLogTitle.Top - 2);

            txtLog.Location = new Point(pad, lblLogTitle.Bottom + 6);
            int logHeight = this.ClientSize.Height - txtLog.Top - 20;
            if (logHeight < 80) logHeight = 80;
            txtLog.Size = new Size(availableWidth, logHeight);
        }

        private void BuildPortalLinks(Panel container)
        {
            container.Controls.Clear();
            int spacing = 12;
            int colWidth = (container.ClientSize.Width - spacing) / 2;
            if (colWidth < 240) colWidth = 240;

            AddLinkCard(container, 0, 0, colWidth, "🎓 Candidate Live Exam Portal", "/livetest", "Candidates open this on any mobile/desktop network");
            AddLinkCard(container, colWidth + spacing, 0, colWidth, "🏆 Candidate Marksheet & Result", "/results", "Official marksheet with supervisor digital signature");
            AddLinkCard(container, 0, 92, colWidth, "🛡️ Supervisor Live Proctor Grid", "/admin", "Real-time WebRTC multi-camera supervisor desk");
            AddLinkCard(container, colWidth + spacing, 92, colWidth, "📊 Analytics & Audit Dashboard", "/dashboard", "Performance graphs, logs, and score summaries");
        }

        private void AddLinkCard(Panel parent, int x, int y, int width, string title, string subpath, string desc)
        {
            Panel p = new Panel
            {
                Location = new Point(x, y),
                Size = new Size(width, 82),
                BackColor = clrCard,
                BorderStyle = BorderStyle.None
            };

            Label lbl = new Label
            {
                Text = title,
                Font = new Font("Segoe UI", 9.5f, FontStyle.Bold),
                ForeColor = Color.White,
                Location = new Point(12, 8),
                AutoSize = true
            };
            p.Controls.Add(lbl);

            Label lblDesc = new Label
            {
                Text = desc,
                Font = new Font("Segoe UI", 7.5f),
                ForeColor = clrSubtext,
                Location = new Point(12, 28),
                AutoSize = true
            };
            p.Controls.Add(lblDesc);

            Button btnCopy = new Button
            {
                Text = "📋 Copy Link",
                Size = new Size(95, 26),
                Location = new Point(12, 48),
                BackColor = Color.FromArgb(51, 65, 85),
                ForeColor = Color.White,
                FlatStyle = FlatStyle.Flat,
                Font = new Font("Segoe UI", 8f),
                Cursor = Cursors.Hand
            };
            btnCopy.FlatAppearance.BorderSize = 0;
            btnCopy.Click += (s, e) =>
            {
                CopyPortalUrl(subpath);
                btnCopy.Text = "✓ Copied!";
                System.Windows.Forms.Timer t = new System.Windows.Forms.Timer { Interval = 1500 };
                t.Tick += (ts, te) => { btnCopy.Text = "📋 Copy Link"; t.Stop(); };
                t.Start();
            };
            p.Controls.Add(btnCopy);

            Button btnOpen = new Button
            {
                Text = "🌐 Open",
                Size = new Size(76, 26),
                Location = new Point(114, 48),
                BackColor = clrAccent,
                ForeColor = Color.White,
                FlatStyle = FlatStyle.Flat,
                Font = new Font("Segoe UI", 8f),
                Cursor = Cursors.Hand
            };
            btnOpen.FlatAppearance.BorderSize = 0;
            btnOpen.Click += (s, e) => OpenPortalUrl(subpath);
            p.Controls.Add(btnOpen);

            parent.Controls.Add(p);
        }

        private void CopyPortalUrl(string subpath)
        {
            if (string.IsNullOrEmpty(activeTunnelUrl))
            {
                MessageBox.Show("Start the tunnel first to get active public links.", "Tunnel Not Active", MessageBoxButtons.OK, MessageBoxIcon.Information);
                return;
            }
            string full = activeTunnelUrl.TrimEnd('/') + subpath;
            try { Clipboard.SetText(full); } catch { }
            Log("Copied to clipboard: " + full);
        }

        private void OpenPortalUrl(string subpath)
        {
            if (string.IsNullOrEmpty(activeTunnelUrl))
            {
                MessageBox.Show("Start the tunnel first to open links.", "Tunnel Not Active", MessageBoxButtons.OK, MessageBoxIcon.Information);
                return;
            }
            string full = activeTunnelUrl.TrimEnd('/') + subpath;
            try { Process.Start(full); } catch (Exception ex) { Log("Error opening browser: " + ex.Message); }
        }

        private void ResolveBinaries()
        {
            // 1. Resolve ADB
            string sdkPath = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), @"Android\Sdk\platform-tools\adb.exe");
            if (File.Exists(sdkPath))
            {
                adbPath = sdkPath;
            }
            else if (File.Exists(Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "adb.exe")))
            {
                adbPath = Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "adb.exe");
            }
            else
            {
                adbPath = "adb.exe";
            }

            // 2. Resolve cloudflared
            string localCf = Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "cloudflared.exe");
            if (File.Exists(localCf))
            {
                cloudflaredPath = localCf;
            }
            else
            {
                cloudflaredPath = "cloudflared.exe";
            }

            Log("ADB Binary: " + adbPath);
            Log("Cloudflare Binary: " + cloudflaredPath);
        }

        private void CheckDevice()
        {
            try
            {
                ProcessStartInfo psi = new ProcessStartInfo
                {
                    FileName = adbPath,
                    Arguments = "devices",
                    RedirectStandardOutput = true,
                    UseShellExecute = false,
                    CreateNoWindow = true
                };
                using (Process p = Process.Start(psi))
                {
                    string output = p.StandardOutput.ReadToEnd();
                    p.WaitForExit();

                    string[] lines = output.Split(new[] { '\r', '\n' }, StringSplitOptions.RemoveEmptyEntries);
                    string connectedDev = "";
                    foreach (string line in lines)
                    {
                        if (line.Contains("\tdevice"))
                        {
                            connectedDev = line.Split('\t')[0].Trim();
                            break;
                        }
                    }

                    if (!string.IsNullOrEmpty(connectedDev))
                    {
                        lblDevice.Text = "🟢 Connected Device: " + connectedDev + " (USB ADB)";
                        lblDevice.ForeColor = clrSuccess;
                    }
                    else
                    {
                        lblDevice.Text = "⚠️ No phone detected via USB. Make sure USB Debugging is ON.";
                        lblDevice.ForeColor = clrWarning;
                    }
                }
            }
            catch (Exception ex)
            {
                lblDevice.Text = "ADB Error: " + ex.Message;
                lblDevice.ForeColor = clrError;
            }
        }

        private void BtnToggle_Click(object sender, EventArgs e)
        {
            if (isRunning)
            {
                StopTunnel();
            }
            else
            {
                StartTunnel();
            }
        }

        private void StartTunnel()
        {
            btnToggle.Enabled = false;
            btnToggle.Text = "⏳ CONNECTING...";
            btnToggle.BackColor = clrWarning;
            lblStatus.Text = "🟡 Connecting to Cloudflare Edge...";
            lblStatus.ForeColor = clrWarning;
            Log("Initializing Internet Tunnel Gateway...");

            new Thread(() =>
            {
                try
                {
                    // 1. Setup ADB port forwarding (PC 8080 -> Phone 8080)
                    Log("Configuring ADB port forwarding (PC:8080 -> Phone:8080)...");
                    ProcessStartInfo adbRevPsi = new ProcessStartInfo
                    {
                        FileName = adbPath,
                        Arguments = "reverse --remove-all",
                        RedirectStandardOutput = true,
                        RedirectStandardError = true,
                        UseShellExecute = false,
                        CreateNoWindow = true
                    };
                    using (Process adbRevProc = Process.Start(adbRevPsi)) { adbRevProc.WaitForExit(); }

                    ProcessStartInfo adbPsi = new ProcessStartInfo
                    {
                        FileName = adbPath,
                        Arguments = "forward tcp:8080 tcp:8080",
                        RedirectStandardOutput = true,
                        RedirectStandardError = true,
                        UseShellExecute = false,
                        CreateNoWindow = true
                    };
                    using (Process adbProc = Process.Start(adbPsi))
                    {
                        adbProc.WaitForExit();
                        Log("ADB port 8080 (PC -> Phone) configured successfully.");
                    }

                    // 2. Launch cloudflared process
                    Log("Spawning Cloudflare edge tunnel on 127.0.0.1:8080...");
                    ProcessStartInfo cfPsi = new ProcessStartInfo
                    {
                        FileName = cloudflaredPath,
                        Arguments = "tunnel --url http://127.0.0.1:8080",
                        RedirectStandardError = true,
                        RedirectStandardOutput = true,
                        UseShellExecute = false,
                        CreateNoWindow = true
                    };

                    tunnelProcess = new Process { StartInfo = cfPsi };
                    tunnelProcess.ErrorDataReceived += (s, ev) =>
                    {
                        if (ev.Data != null)
                        {
                            OnTunnelOutput(ev.Data);
                        }
                    };
                    tunnelProcess.OutputDataReceived += (s, ev) =>
                    {
                        if (ev.Data != null)
                        {
                            OnTunnelOutput(ev.Data);
                        }
                    };

                    tunnelProcess.Start();
                    tunnelProcess.BeginErrorReadLine();
                    tunnelProcess.BeginOutputReadLine();

                    isRunning = true;
                }
                catch (Exception ex)
                {
                    this.Invoke((MethodInvoker)delegate
                    {
                        lblStatus.Text = "🔴 Failed to Start";
                        lblStatus.ForeColor = clrError;
                        btnToggle.Text = "🚀 START TUNNEL";
                        btnToggle.BackColor = clrSuccess;
                        btnToggle.Enabled = true;
                        Log("Error launching tunnel: " + ex.Message);
                        MessageBox.Show("Error starting tunnel:\n" + ex.Message, "Gateway Error", MessageBoxButtons.OK, MessageBoxIcon.Error);
                    });
                }
            }).Start();
        }

        private void OnTunnelOutput(string line)
        {
            // Scan for trycloudflare URL
            Match m = Regex.Match(line, @"(https://[a-zA-Z0-9-]+\.trycloudflare\.com)");
            if (m.Success && string.IsNullOrEmpty(activeTunnelUrl))
            {
                activeTunnelUrl = m.Groups[1].Value;

                this.Invoke((MethodInvoker)delegate
                {
                    lblStatus.Text = "🟢 Active & Relaying (Online)";
                    lblStatus.ForeColor = clrSuccess;
                    lblPublicUrl.Text = activeTunnelUrl;
                    lblPublicUrl.ForeColor = clrSuccess;
                    btnToggle.Text = "⏹️ STOP TUNNEL";
                    btnToggle.BackColor = clrError;
                    btnToggle.Enabled = true;

                    // Copy to clipboard
                    try { Clipboard.SetText(activeTunnelUrl); } catch { }

                    notifyIcon.ShowBalloonTip(3000, "QPby64 Gateway Active", "Public URL: " + activeTunnelUrl + "\nAuto-synced to phone!", ToolTipIcon.Info);

                    Log(">>> LIVE INTERNET URL OBTAINED: " + activeTunnelUrl);
                    Log(">>> Base URL copied to clipboard.");
                });

                // Broadcast to Android Phone over ADB
                new Thread(() =>
                {
                    try
                    {
                        ProcessStartInfo bcPsi = new ProcessStartInfo
                        {
                            FileName = adbPath,
                            Arguments = "shell am broadcast -a com.example.SET_PUBLIC_TUNNEL_URL -n com.aistudio.questionbank.v1.agqpby64/com.example.service.TunnelUrlReceiver --es url \"" + activeTunnelUrl + "\"",
                            RedirectStandardOutput = true,
                            UseShellExecute = false,
                            CreateNoWindow = true
                        };
                        using (Process bcProc = Process.Start(bcPsi))
                        {
                            bcProc.WaitForExit();
                            Log(">>> Synced public URL directly to phone QPby64 app via ADB broadcast.");
                        }
                    }
                    catch (Exception ex)
                    {
                        Log("Could not broadcast to phone: " + ex.Message);
                    }
                }).Start();
            }

            // Append interesting logs
            if (line.Contains("HTTP/") || line.Contains("ERR") || line.Contains("INF Registered"))
            {
                this.Invoke((MethodInvoker)delegate { Log(line); });
            }
        }

        private void StopTunnel()
        {
            Log("Stopping tunnel process...");
            try
            {
                if (tunnelProcess != null && !tunnelProcess.HasExited)
                {
                    tunnelProcess.Kill();
                }
            }
            catch { }
            tunnelProcess = null;
            isRunning = false;
            activeTunnelUrl = "";

            lblStatus.Text = "🔴 Offline - Ready to Connect";
            lblStatus.ForeColor = clrError;
            lblPublicUrl.Text = "Waiting for tunnel to start...";
            lblPublicUrl.ForeColor = Color.FromArgb(203, 213, 225);
            btnToggle.Text = "🚀 START TUNNEL";
            btnToggle.BackColor = clrSuccess;
            btnToggle.Enabled = true;
            Log("Tunnel stopped. Gateway is offline.");
        }

        private void Log(string msg)
        {
            if (txtLog.IsDisposed) return;
            string stamp = DateTime.Now.ToString("HH:mm:ss");
            txtLog.AppendText(string.Format("[{0}] {1}\r\n", stamp, msg));
        }

        private void RestoreWindow()
        {
            this.Show();
            this.WindowState = FormWindowState.Normal;
            this.BringToFront();
        }

        private void ExitApp()
        {
            StopTunnel();
            notifyIcon.Visible = false;
            Application.Exit();
        }

        private void MainForm_FormClosing(object sender, FormClosingEventArgs e)
        {
            if (isRunning && e.CloseReason == CloseReason.UserClosing)
            {
                DialogResult res = MessageBox.Show(
                    "The Internet Tunnel is currently active. Do you want to minimize it to the System Tray to keep remote candidates connected?",
                    "Keep Tunnel Running?",
                    MessageBoxButtons.YesNoCancel,
                    MessageBoxIcon.Question
                );

                if (res == DialogResult.Yes)
                {
                    e.Cancel = true;
                    this.Hide();
                    notifyIcon.ShowBalloonTip(2000, "QPby64 Running in Background", "Tunnel is active in the system tray.", ToolTipIcon.Info);
                    return;
                }
                else if (res == DialogResult.Cancel)
                {
                    e.Cancel = true;
                    return;
                }
            }
            StopTunnel();
            notifyIcon.Visible = false;
        }
    }
}
