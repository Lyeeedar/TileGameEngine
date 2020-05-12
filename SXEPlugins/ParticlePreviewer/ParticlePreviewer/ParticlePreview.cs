using StructuredXmlEditor.Plugin.Interfaces;
using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Diagnostics;
using System.IO;
using System.Runtime.CompilerServices;
using System.Text;
using System.Threading.Tasks;
using System.Windows;

namespace ParticlePreviewer
{
	public class ParticlePreview : INotifyPropertyChanged, IResourceViewProvider
	{
		public string CurrentStep
		{
			get { return m_currentStep; }
			set
			{
				m_currentStep = value;
				RaisePropertyChangedEvent();
			}
		}
		private string m_currentStep;

		FrameworkElement view;

		dynamic Workspace;
		public ParticlePreview(object workspace)
		{
			Workspace = workspace;
		}

		public FrameworkElement GetView()
		{
			if (view == null)
			{
				view = new ParticlePreviewView();
				view.DataContext = this;

				Task.Run(() => 
				{
					CompileViewer();
				});
			}

			return view;
		}

		public void CompileViewer()
		{
			var projectRoot = (string)Workspace.ProjectFolder;

			var rootFolder = Path.GetFullPath(Path.Combine(projectRoot, "../.."));
			var gradle = Path.Combine(rootFolder, "gradlew.bat");

			var assetsFolder = Path.GetFullPath(Path.Combine(projectRoot, "../assets"));

			var viewerPath = Path.Combine(assetsFolder, "particleViewer.jar");
			if (!File.Exists(viewerPath))
			{
				CurrentStep = "Building Viewer";
				RunProcess(gradle, new string[] { ":desktop:particlePreviewDist" }, rootFolder, (message) => { });
				File.Copy(Path.Combine(rootFolder, "engine", "desktop", "build", "libs", "desktop.jar"), viewerPath);
			}
			CurrentStep = "Viewer Found";

			var compilerPath = Path.Combine(assetsFolder, "compiler.jar");
			if (!File.Exists(compilerPath))
			{
				CurrentStep = "Building Compiler";

				RunProcess(gradle, new string[] { ":headless:compilerDist" }, rootFolder, (message) => { });
				File.Copy(Path.Combine(rootFolder, "engine", "headless", "build", "libs", "headless.jar"), compilerPath);
			}
			CurrentStep = "Compiler Found";

			CurrentStep = "Setup done";
			Task.Run(() =>
			{
				var messages = new List<string>();
				var exit = RunProcess("java", new string[] { "-jar", viewerPath }, assetsFolder, (message) => { messages.Add(message); });
				if (exit != 0)
				{
					CurrentStep = String.Join("\n", messages);
				}
			});
		}

		public static int RunProcess(
			string programPath,
			string[] cliArgs,
			string workingDirectory,
			Action<string> log,
			Action<string> stdOutHandler = null,
			Action<string> stdErrHandler = null)
		{
			log($"Running process at path: {programPath} with args: {string.Join(" ", cliArgs)}");
			var startInfo = new ProcessStartInfo
			{
				FileName = programPath,
				CreateNoWindow = true,
				RedirectStandardOutput = true,
				RedirectStandardError = true,
				UseShellExecute = false,
				WorkingDirectory = workingDirectory
			};

			foreach (var parameter in cliArgs)
			{
				startInfo.ArgumentList.Add(parameter);
			}

			using var process = new Process { StartInfo = startInfo };

			process.OutputDataReceived += (sender, args) =>
			{
				if (args.Data != null)
				{
					stdOutHandler?.Invoke(args.Data);
					log(args.Data);
				}
			};

			process.ErrorDataReceived += (sender, args) =>
			{
				if (args.Data != null)
				{
					stdErrHandler?.Invoke(args.Data);
					log(args.Data);
				}
			};

			process.EnableRaisingEvents = true;
			process.Start();

			process.BeginOutputReadLine();
			process.BeginErrorReadLine();

			process.WaitForExit();

			log($"exited with code {process.ExitCode}");

			return process.ExitCode;
		}

		public bool ShowForResourceType(string resourceType)
		{
			return resourceType == "Effect";
		}

		//--------------------------------------------------------------------------
		public event PropertyChangedEventHandler PropertyChanged;

		//-----------------------------------------------------------------------
		public void RaisePropertyChangedEvent
		(
			[CallerMemberName] string i_propertyName = ""
		)
		{
			if (PropertyChanged != null)
			{
				PropertyChanged(this, new PropertyChangedEventArgs(i_propertyName));
			}
		}
	}
}
