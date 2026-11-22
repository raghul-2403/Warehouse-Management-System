
using System;
using System.Diagnostics;
using System.Drawing;
using System.IO;
using System.Net.Http;
using System.Threading.Tasks;
using System.Windows.Forms;
using Microsoft.Web.WebView2.Core;
using Microsoft.Web.WebView2.WinForms;

namespace WMSDesktop;

internal static class Program
{
    [STAThread]
    static void Main()
    {
        ApplicationConfiguration.Initialize();
        Application.Run(new WmsWindow());
    }
}

public class WmsWindow : Form
{
    private readonly WebView2 web = new();
    private readonly Label status = new();
    
private readonly FlowLayoutPanel toolbar = new();

private Button backButton = new();
private Button forwardButton = new();

private Button CreateNavButton(string title, Action action)
{
    var button = new Button
    {
        Text = title,
        Width = title.Length > 9 ? 110 : 90,
        Height = 36,
        FlatStyle = FlatStyle.Flat,
        BackColor = Color.FromArgb(37, 55, 80),
        ForeColor = Color.White,
        Margin = new Padding(5, 6, 5, 6),
        Cursor = Cursors.Hand
    };

    button.FlatAppearance.BorderSize = 0;
    button.Click += (_, _) => action();
    return button;
}

private void NavigateTo(string path)
{
    if (web.CoreWebView2 != null)
        web.CoreWebView2.Navigate(
            "http://localhost:8080" + path
        );
}

    private Process? server;

    private const string Url = "http://localhost:8080/";

    public WmsWindow()
    {
        Text = "Warehouse Management System";
        Size = new Size(1280, 800);
        MinimumSize = new Size(900, 600);
        StartPosition = FormStartPosition.CenterScreen;

        status.Text = "Starting Warehouse Management System...";
        status.TextAlign = ContentAlignment.MiddleCenter;
        status.Dock = DockStyle.Fill;

        web.Dock = DockStyle.Fill;
        web.Visible = false;

        Controls.Add(web);
        Controls.Add(status);

        
toolbar.Dock = DockStyle.Top;
toolbar.Height = 50;
toolbar.BackColor = Color.FromArgb(18, 32, 54);
toolbar.Padding = new Padding(8, 0, 8, 0);
toolbar.WrapContents = false;

backButton = CreateNavButton("← Back", () =>
{
    if (web.CanGoBack)
        web.GoBack();
});

forwardButton = CreateNavButton("Forward →", () =>
{
    if (web.CanGoForward)
        web.GoForward();
});

toolbar.Controls.Add(backButton);
toolbar.Controls.Add(forwardButton);

toolbar.Controls.Add(
    CreateNavButton("Home", () => NavigateTo("/"))
);

toolbar.Controls.Add(
    CreateNavButton("Inventory", () => NavigateTo("/inventory"))
);

toolbar.Controls.Add(
    CreateNavButton("Orders", () => NavigateTo("/orders"))
);

toolbar.Controls.Add(
    CreateNavButton("Refresh", () => web.Reload())
);

Controls.Add(toolbar);

backButton.Enabled = false;
forwardButton.Enabled = false;

web.NavigationCompleted += (_, _) =>
{
    backButton.Enabled = web.CanGoBack;
    forwardButton.Enabled = web.CanGoForward;
};


        Shown += async (_, _) => await StartWms();
        FormClosing += (_, _) => StopOwnedServer();
    }

    private async Task StartWms()
    {
        string folder = AppContext.BaseDirectory;
        string jar = Path.Combine(folder, "WMS.jar");

        if (!File.Exists(jar))
        {
            MessageBox.Show("WMS.jar not found.");
            Close();
            return;
        }

        try
        {
            if (!await IsReady())
            {
                string? javaHome =
                    Environment.GetEnvironmentVariable("JAVA_HOME");

                string java = Path.Combine(
                    javaHome ?? "",
                    "bin",
                    "javaw.exe"
                );

                if (!File.Exists(java))
                    java = @"C:\Program Files\Java\jdk-17\bin\javaw.exe";

                if (!File.Exists(java))
                    throw new Exception("Java 17 not found.");

                server = Process.Start(new ProcessStartInfo
                {
                    FileName = java,
                    Arguments = "-jar \"WMS.jar\"",
                    WorkingDirectory = folder,
                    UseShellExecute = false,
                    CreateNoWindow = true
                });
            }

            bool ready = false;

            for (int i = 0; i < 90; i++)
            {
                if (await IsReady())
                {
                    ready = true;
                    break;
                }

                if (server?.HasExited == true)
                    break;

                await Task.Delay(1000);
            }

            if (!ready)
                throw new Exception(
                    "WMS could not start. Check MySQL and port 8080."
                );

            string profile = Path.Combine(
                Environment.GetFolderPath(
                    Environment.SpecialFolder.LocalApplicationData),
                "WMSDesktop",
                "WebView2"
            );

            Directory.CreateDirectory(profile);

            var environment =
                await CoreWebView2Environment.CreateAsync(
                    null, profile
                );

            await web.EnsureCoreWebView2Async(environment);

            web.CoreWebView2.NewWindowRequested += (_, e) =>
            {
                e.Handled = true;

                if (e.Uri.StartsWith(
                    Url, StringComparison.OrdinalIgnoreCase))
                {
                    web.CoreWebView2.Navigate(e.Uri);
                }
                else
                {
                    Process.Start(new ProcessStartInfo(e.Uri)
                    {
                        UseShellExecute = true
                    });
                }
            };

            web.Source = new Uri(Url);
            web.Visible = true;
            status.Visible = false;
        }
        catch (Exception ex)
        {
            MessageBox.Show(
                ex.Message,
                "WMS Startup Error",
                MessageBoxButtons.OK,
                MessageBoxIcon.Error
            );

            Close();
        }
    }

    private static async Task<bool> IsReady()
    {
        try
        {
            using var client = new HttpClient(
                new HttpClientHandler
                {
                    AllowAutoRedirect = false
                });

            client.Timeout = TimeSpan.FromSeconds(1);

            using var response = await client.GetAsync(Url);

            return (int)response.StatusCode < 500;
        }
        catch
        {
            return false;
        }
    }

    private void StopOwnedServer()
    {
        try
        {
            if (server != null && !server.HasExited)
                server.Kill();
        }
        catch
        {
            // Server may already be stopped.
        }
    }
}
