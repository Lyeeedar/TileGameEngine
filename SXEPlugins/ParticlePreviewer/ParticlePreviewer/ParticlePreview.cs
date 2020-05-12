using StructuredXmlEditor.Plugin.Interfaces;
using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Diagnostics;
using System.IO;
using System.Runtime.CompilerServices;
using System.Text;
using System.Threading;
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

		public ExternalWindowHost WindowHost { get; set; }

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

				var srcPath = Path.Combine(rootFolder, "engine", "desktop", "build", "libs", "desktop.jar");
				if (File.Exists(srcPath)) File.Delete(srcPath);

				RunProcess(gradle, new string[] { ":desktop:particlePreviewDist" }, rootFolder);
				File.Copy(srcPath, viewerPath);
			}
			CurrentStep = "Viewer Found";

			var compilerPath = Path.Combine(assetsFolder, "compiler.jar");
			if (!File.Exists(compilerPath))
			{
				CurrentStep = "Building Compiler";

				var srcPath = Path.Combine(rootFolder, "engine", "headless", "build", "libs", "headless.jar");
				if (File.Exists(srcPath)) File.Delete(srcPath);

				RunProcess(gradle, new string[] { ":headless:compilerDist" }, rootFolder);
				File.Copy(srcPath, compilerPath);
			}
			CurrentStep = "Compiler Found";

			CurrentStep = "Setup done";
			Task.Run(() =>
			{
				var startInfo = new ProcessStartInfo
				{
					FileName = "javaw",
					UseShellExecute = false,
					WorkingDirectory = assetsFolder
				};
				startInfo.ArgumentList.Add("-jar");
				startInfo.ArgumentList.Add(viewerPath);

				using var process = new Process { StartInfo = startInfo };
				process.Start();
				process.WaitForInputIdle();
				var id = process.Id;

				Thread.Sleep(2000);

				process.Refresh();

				Application.Current.Dispatcher.Invoke(() =>
				{
					WindowHost = new ExternalWindowHost(id);
					RaisePropertyChangedEvent(nameof(WindowHost));
				});
			});
		}

		public static int RunProcess(string programPath, string[] cliArgs,string workingDirectory)
		{
			var startInfo = new ProcessStartInfo
			{
				FileName = programPath,
				CreateNoWindow = true,
				UseShellExecute = false,
				WorkingDirectory = workingDirectory
			};

			foreach (var parameter in cliArgs)
			{
				startInfo.ArgumentList.Add(parameter);
			}

			using var process = new Process { StartInfo = startInfo };

			process.EnableRaisingEvents = true;
			process.Start();

			process.WaitForExit();

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
