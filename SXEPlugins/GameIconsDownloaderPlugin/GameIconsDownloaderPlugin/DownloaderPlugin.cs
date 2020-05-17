using StructuredXmlEditor.Plugin.Interfaces;
using Svg;
using System;
using System.IO;
using System.IO.Compression;
using System.Net;
using System.Text.RegularExpressions;
using System.Threading.Tasks;

namespace GameIconsDownloaderPlugin
{
	public class DownloaderPlugin : IMenuItemProvider
	{
		dynamic Workspace { get; }

		public DownloaderPlugin(object workspace)
		{
			Workspace = workspace;
		}

		public Tuple<string, Action> GetMenuItem()
		{
			return new Tuple<string, Action>("Download icons from GameIcons.net", () => { Download(); });
		}

		private void Download()
		{
			string projectFolder = Workspace.ProjectFolder;
			var outputPath = Path.Combine(projectFolder, "Sprites", "GameIconsRaw");

			ParseTagsPage(outputPath);
		}

		private void ParseTagsPage(string outputFolder)
		{
			var contents = FetchPageContent("https://game-icons.net/tags.html");

			var regex = new Regex("<a href=\"/tags/.*?\\.html\"");
			var matches = regex.Matches(contents);

			var tempFolder = Path.Combine(Path.GetTempPath(), "svg-icons");

			if (Directory.Exists(tempFolder))
			{
				foreach (var file in Directory.EnumerateFiles(tempFolder, "*", SearchOption.AllDirectories))
				{
					File.Delete(file);
				}
				Directory.Delete(tempFolder, true);
			}
			Directory.CreateDirectory(tempFolder);

			if (Directory.Exists(outputFolder))
			{
				foreach (var file in Directory.EnumerateFiles(outputFolder, "*", SearchOption.AllDirectories))
				{
					File.Delete(file);
				}
				Directory.Delete(outputFolder, true);
			}
			Directory.CreateDirectory(outputFolder);

			var n = 0;
			Parallel.ForEach(matches, (match) =>
			{
				var tag = match.ToString().Split("/tags/")[1].Split(".html")[0];
				DownloadTags(tag, tempFolder, outputFolder);

				Console.WriteLine($"Completed {n}/{matches.Count}");
				n++;
			});
		}

		private string FetchPageContent(string url)
		{
			try
			{
				HttpWebRequest request = (HttpWebRequest)WebRequest.Create(url);

				using (HttpWebResponse response = (HttpWebResponse)request.GetResponse())
				using (Stream stream = response.GetResponseStream())
				using (StreamReader reader = new StreamReader(stream))
				{
					var html = reader.ReadToEnd();
					return html;
				}
			}
			catch (Exception)
			{
				return null;
			}
		}

		public void DownloadTags(string tag, string tempFolder, string outputRootFolder)
		{
			Console.WriteLine("Downloading tag: " + tag);

			var url = $"https://game-icons.net/archives/svg/zip/ffffff/transparent/{tag}.svg.zip";

			var downloadLocation = Path.Combine(tempFolder, tag + ".zip");
			using (var client = new WebClient())
			{
				client.DownloadFile(url, downloadLocation);
			}

			Console.WriteLine("Extracting zip");
			var extractLocation = Path.Combine(tempFolder, tag);
			ZipFile.ExtractToDirectory(downloadLocation, extractLocation);

			var outputFolder = Path.Combine(outputRootFolder, tag);
			Directory.CreateDirectory(outputFolder);

			foreach (var svgFile in Directory.EnumerateFiles(extractLocation, "*.svg", SearchOption.AllDirectories))
			{
				var svgDoc = SvgDocument.Open<SvgDocument>(svgFile, null);
				svgDoc.Width = 48;
				svgDoc.Height = 48;

				var bitmap = svgDoc.Draw();

				var fileName = Path.GetFileNameWithoutExtension(svgFile);
				var outputPath = Path.Combine(outputFolder, fileName + ".png");

				if (File.Exists(outputPath))
				{
					var i = 1;
					while (File.Exists(outputPath))
					{
						outputPath = Path.Combine(outputFolder, fileName + "-" + i++ + ".png");
					}
				}

				bitmap.Save(outputPath);
			}
		}
	}
}
