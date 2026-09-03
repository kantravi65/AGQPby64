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
        private Label lblStatus;
        private Label lblPublicUrl;
        private Label lblDevice;
        private TextBox txtLog;
        private Panel panelHeader;
        private Panel panelLinks;
        private NotifyIcon notifyIcon;
        private ContextMenuStrip trayMenu;
        private System.Windows.Forms.Timer deviceTimer;

        private Process tunnelProcess;
        private string activeTunnelUrl = "";
        private bool isRunning = false;
        private string adbPath = "";
        private string cloudflaredPath = "";

        // UI Colors
        private Color clrBg = Color.FromArgb(15, 23, 42);          // Slate 900
        private Color clrCard = Color.FromArgb(30, 41, 59);        // Slate 800
        private Color clrCardBorder = Color.FromArgb(51, 65, 85);  // Slate 700
        private Color clrAccent = Color.FromArgb(14, 165, 233);    // Sky 500
        private Color clrSuccess = Color.FromArgb(16, 185, 129);   // Emerald 500
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
            this.Size = new Size(820, 680);
            this.MinimumSize = new Size(760, 600);
            this.StartPosition = FormStartPosition.CenterScreen;
            this.BackColor = clrBg;
            this.ForeColor = clrText;
            this.Font = new Font("Segoe UI", 9.5f, FontStyle.Regular);
            this.DoubleBuffered = true;

            // Header Panel
            panelHeader = new Panel
            {
                Dock = DockStyle.Top,
                Height = 85,
                BackColor = Color.FromArgb(10, 15, 30),
                Padding = new Padding(24, 16, 24, 16)
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

            // Main Layout Container
            Panel panelContent = new Panel
            {
                Dock = DockStyle.Fill,
                Padding = new Padding(20),
                AutoScroll = true
            };
            this.Controls.Add(panelContent);

            int currentY = 10;

            // Status & Device Card
            Panel cardStatus = CreateCard(currentY, 110);
            
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

            Label lblStatTitle = new Label
            {
                Text = "GATEWAY STATUS:",
                Font = new Font("Segoe UI", 8f, FontStyle.Bold),
                ForeColor = clrSubtext,
                Location = new Point(16, 64),
                AutoSize = true
            };
            cardStatus.Controls.Add(lblStatTitle);

            lblStatus = new Label
            {
                Text = "🔴 Offline - Ready to Connect",
                Font = new Font("Segoe UI", 10.5f, FontStyle.Bold),
                ForeColor = clrError,
                Location = new Point(16, 82),
                AutoSize = true
            };
            cardStatus.Controls.Add(lblStatus);

            // Connect Button inside Card
            btnToggle = new Button
            {
                Text = "🚀 START TUNNEL",
                Font = new Font("Segoe UI", 11f, FontStyle.Bold),
                BackColor = clrSuccess,
                ForeColor = Color.White,
                FlatStyle = FlatStyle.Flat,
                Size = new Size(200, 52),
                Location = new Point(cardStatus.Width - 220, 28),
                Anchor = AnchorStyles.Top | AnchorStyles.Right,
                Cursor = Cursors.Hand
            };
            btnToggle.FlatAppearance.BorderSize = 0;
            btnToggle.Click += BtnToggle_Click;
            cardStatus.Controls.Add(btnToggle);

            panelContent.Controls.Add(cardStatus);
            currentY += 122;

            // Public Tunnel Active URL Card
            Panel cardUrl = CreateCard(currentY, 70);
            Label lblUrlTitle = new Label
            {
                Text = "PUBLIC TUNNEL BASE URL (COPIED TO CLIPBOARD):",
                Font = new Font("Segoe UI", 8f, FontStyle.Bold),
                ForeColor = clrSubtext,
                Location = new Point(16, 12),
                AutoSize = true
            };
            cardUrl.Controls.Add(lblUrlTitle);

            lblPublicUrl = new Label
            {
                Text = "Waiting for tunnel to start...",
                Font = new Font("Consolas", 11.5f, FontStyle.Bold),
                ForeColor = Color.FromArgb(203, 213, 225),
                Location = new Point(16, 34),
                AutoSize = true
            };
            cardUrl.Controls.Add(lblPublicUrl);
            panelContent.Controls.Add(cardUrl);
            currentY += 82;

            // Portal Links Cards Panel
            panelLinks = new Panel
            {
                Location = new Point(20, currentY),
                Width = this.ClientSize.Width - 40,
                Height = 180,
                Anchor = AnchorStyles.Top | AnchorStyles.Left | AnchorStyles.Right
            };
            BuildPortalLinks(panelLinks);
            panelContent.Controls.Add(panelLinks);
            currentY += 190;

            // Log Label
            Label lblLogTitle = new Label
            {
                Text = "LIVE GATEWAY ACTIVITY & EVENT LOG:",
                Font = new Font("Segoe UI", 8f, FontStyle.Bold),
                ForeColor = clrSubtext,
                Location = new Point(20, currentY),
                AutoSize = true
            };
            panelContent.Controls.Add(lblLogTitle);
            currentY += 22;

            // Activity Log Box
            txtLog = new TextBox
            {
                Multiline = true,
                ReadOnly = true,
                ScrollBars = ScrollBars.Vertical,
                BackColor = Color.FromArgb(8, 12, 24),
                ForeColor = Color.FromArgb(226, 232, 240),
                Font = new Font("Consolas", 9f),
                Location = new Point(20, currentY),
                Size = new Size(this.ClientSize.Width - 40, 130),
                Anchor = AnchorStyles.Top | AnchorStyles.Bottom | AnchorStyles.Left | AnchorStyles.Right,
                BorderStyle = BorderStyle.FixedSingle
            };
            panelContent.Controls.Add(txtLog);

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
        }

        private Panel CreateCard(int top, int height)
        {
            return new Panel
            {
                Location = new Point(20, top),
                Width = this.ClientSize.Width - 40,
                Height = height,
                BackColor = clrCard,
                Anchor = AnchorStyles.Top | AnchorStyles.Left | AnchorStyles.Right,
                Padding = new Padding(12)
            };
        }

        private void BuildPortalLinks(Panel container)
        {
            container.Controls.Clear();
            int colWidth = (container.Width - 30) / 2;

            AddLinkCard(container, 0, 0, colWidth, "🎓 Candidate Live Exam Portal", "/livetest", "Candidates open this in browser on any network");
            AddLinkCard(container, colWidth + 15, 0, colWidth, "🏆 Candidate Marksheet & Result", "/results", "Official marksheet with supervisor digital signature");
            AddLinkCard(container, 0, 88, colWidth, "🛡️ Supervisor Live Proctor Grid", "/admin", "Real-time WebRTC multi-camera supervision");
            AddLinkCard(container, colWidth + 15, 88, colWidth, "📊 Analytics & Audit Dashboard", "/dashboard", "Performance graphs, logs, and score summaries");
        }

        private void AddLinkCard(Panel parent, int x, int y, int width, string title, string subpath, string desc)
        {
            Panel p = new Panel
            {
                Location = new Point(x, y),
                Size = new Size(width, 78),
                BackColor = clrCard,
                BorderStyle = BorderStyle.None
            };

            Label lbl = new Label
            {
                Text = title,
                Font = new Font("Segoe UI", 9.5f, FontStyle.Bold),
                ForeColor = Color.White,
                Location = new Point(10, 8),
                AutoSize = true
            };
            p.Controls.Add(lbl);

            Label lblDesc = new Label
            {
                Text = desc,
                Font = new Font("Segoe UI", 7.5f),
                ForeColor = clrSubtext,
                Location = new Point(10, 28),
                AutoSize = true
            };
            p.Controls.Add(lblDesc);

            Button btnCopy = new Button
            {
                Text = "📋 Copy Link",
                Size = new Size(88, 26),
                Location = new Point(10, 46),
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
                Size = new Size(72, 26),
                Location = new Point(104, 46),
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
            Clipboard.SetText(full);
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
                        lblDevice.ForeColor = Color.FromArgb(234, 179, 8); // Yellow 500
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
            btnToggle.Text = "CONNECTING...";
            btnToggle.BackColor = Color.FromArgb(202, 138, 4); // Yellow
            lblStatus.Text = "🟡 Connecting to Cloudflare Edge...";
            lblStatus.ForeColor = Color.FromArgb(234, 179, 8);
            Log("Initializing Internet Tunnel Gateway...");

            new Thread(() =>
            {
                try
                {
                    // 1. Setup ADB reverse port forwarding (8080)
                    Log("Configuring ADB reverse port forwarding for 8080...");
                    ProcessStartInfo adbPsi = new ProcessStartInfo
                    {
                        FileName = adbPath,
                        Arguments = "reverse tcp:8080 tcp:8080",
                        RedirectStandardOutput = true,
                        RedirectStandardError = true,
                        UseShellExecute = false,
                        CreateNoWindow = true
                    };
                    using (Process adbProc = Process.Start(adbPsi))
                    {
                        adbProc.WaitForExit();
                        Log("ADB reverse port 8080 configured successfully.");
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
                            Arguments = "shell am broadcast -a com.example.SET_PUBLIC_TUNNEL_URL --es url \"" + activeTunnelUrl + "\"",
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
