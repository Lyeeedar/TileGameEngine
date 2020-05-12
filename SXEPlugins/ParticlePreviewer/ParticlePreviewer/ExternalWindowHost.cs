using Microsoft.DwayneNeed.Interop;
using Microsoft.DwayneNeed.Win32;
using Microsoft.DwayneNeed.Win32.User32;
using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Runtime.InteropServices;
using System.Text;
using System.Threading;
using System.Windows.Interop;

namespace ParticlePreviewer
{
	public class ExternalWindowHost : HwndHostEx
	{
		int processID;
		public ExternalWindowHost(int processID)
		{
			this.processID = processID;
		}

		protected override HWND BuildWindowOverride(HWND hwndParent)
		{
			var process = Process.GetProcessById(processID);
			HWND hwnd = new HWND(process.MainWindowHandle);

			int style = NativeMethods.GetWindowLong(hwnd, GWL.STYLE);

			style = style & ~((int)WS.CAPTION) & ~((int)WS.THICKFRAME); // Removes Caption bar and the sizing border
			style |= ((int)WS.CHILD); // Must be a child window to be hosted

			NativeMethods.SetWindowLong(hwnd, GWL.STYLE, style);

			return hwnd;
		}

		protected override void DestroyWindowOverride(HWND hwnd)
		{
			var process = Process.GetProcessById(processID);

			process.CloseMainWindow();

			process.WaitForExit(5000);

			if (process.HasExited == false)
			{
				process.Kill();
			}

			process.Close();
			process.Dispose();

			hwnd.Dispose();
			hwnd = null;
		}
	}
}
