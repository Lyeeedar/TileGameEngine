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
using System.Xml.Linq;

namespace InGamePreviewPlugin
{
	public class InGamePreview : IResourceViewProvider
	{
		private List<ViewerDef> GameViewerDefs = new List<ViewerDef>();
		private ViewerDef CurrentDef;

		dynamic Workspace;
		public InGamePreview(object workspace)
		{
			Workspace = workspace;

			ParserViewerDefs();
		}

		public void ParserViewerDefs()
		{
			var projectRoot = (string)Workspace.ProjectFolder;

			var defsFile = Path.GetFullPath(Path.Combine(projectRoot, "SXEPlugins", "ViewerDefs.txt"));

			if (File.Exists(defsFile))
			{
				var contents = File.ReadAllLines(defsFile);
				foreach (var line in contents)
				{
					var split = line.Split(',');
					var def = new ViewerDef(split[0], split[1], split[2], split[3]);
					GameViewerDefs.Add(def);
				}
			}
		}

		public FrameworkElement GetView()
		{
			if (CurrentDef != null)
			{
				if (CurrentDef.View == null)
				{
					CurrentDef.View = new InGamePreviewView(Workspace, CurrentDef);
				}

				return CurrentDef.View;
			}

			return null;
		}

		public bool ShowForResourceType(string resourceType)
		{
			if (CurrentDef?.ResourceType == resourceType)
			{
				return true;
			}

			CurrentDef = null;

			foreach (var def in GameViewerDefs)
			{
				if (def.ResourceType == resourceType)
				{
					CurrentDef = def;
					break;
				}
			}

			return CurrentDef != null;
		}
	}

	public class ViewerDef
	{
		public string BuildTask { get; }
		public string ResourceType { get; }
		public string ViewerName { get; }
		public string TempResourceName { get; }

		public FrameworkElement View { get; set; }

		public ViewerDef(string buildTask, string resourceType, string viewerName, string tempResourceName)
		{
			this.BuildTask = buildTask;
			this.ResourceType = resourceType;
			this.ViewerName = viewerName;
			this.TempResourceName = tempResourceName;
		}
	}
}
