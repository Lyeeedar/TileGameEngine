using System;
using System.Collections.Generic;
using System.Text;
using System.Timers;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Media;

namespace InGamePreviewPlugin
{
	public class LoadingIndicator : FrameworkElement
	{
		public string Text
		{
			get { return (string)GetValue(TextProperty); }
			set { SetValue(TextProperty, value); }
		}
		public static readonly DependencyProperty TextProperty =
			DependencyProperty.Register(nameof(Text), typeof(string), typeof(LoadingIndicator), new FrameworkPropertyMetadata(null, FrameworkPropertyMetadataOptions.AffectsRender));

		const int PipCount = 16;
		const double PipIncrement = 1.0 / PipCount;

		double progress = 0.0;

		public LoadingIndicator()
		{
			var timer = new Timer();
			timer.Interval = 200;
			timer.Elapsed += (e, args) => 
			{
				Application.Current?.Dispatcher?.Invoke(() =>
				{
					if (IsVisible) InvalidateVisual();
				});
			};
			timer.Start();
		}

		DateTime lastTime;
		protected override void OnRender(DrawingContext drawingContext)
		{
			var time = DateTime.Now;
			var delta = time - lastTime;
			lastTime = time;

			progress += delta.TotalSeconds / 2f;
			if (progress > 1)
			{
				progress = 0;
			}

			var squareWidth = Math.Min(ActualWidth, ActualHeight);
			squareWidth = Math.Min(256, squareWidth);
			var finalPipSize = squareWidth * 0.07;
			var centrePoint = new Point(ActualWidth / 2, ActualHeight / 2);
			double ringRadius = (squareWidth / 2) - (finalPipSize * 1.1) - (squareWidth * 0.1);

			var progressVector = (Vector)new RotateTransform(360.0 * progress, 0, 0).Transform(new Point(0, -1));
			var indicatorPosition = new TranslateTransform(centrePoint.X, centrePoint.Y - ringRadius);

			for (int i = 0; i < PipCount; i++)
			{
				var transform = new TransformGroup();
				transform.Children.Add(indicatorPosition);
				transform.Children.Add(new RotateTransform(360.0 * i * PipIncrement, centrePoint.X, centrePoint.Y));

				var progressDot = Vector.Multiply(progressVector, (Vector)new RotateTransform(360.0 * i * PipIncrement, 0, 0).Transform(new Point(0, -1)));
				double pulse = 1;
				if (progressDot > 0.8)
				{
					pulse += ((progressDot - 0.8) / 0.2) * 0.5;
				}
				var pipRadius = (finalPipSize * pulse) / 2;
				drawingContext.DrawEllipse(Brushes.DarkGreen, null, transform.Transform(new Point()), pipRadius, pipRadius);
			}

			if (!string.IsNullOrEmpty(Text) && ActualWidth > 0 && ActualHeight > 0)
			{
				var text = new FormattedText(Text, System.Globalization.CultureInfo.CurrentUICulture, GetFlowDirection(this),
					new Typeface(TextBlock.GetFontFamily(this), TextBlock.GetFontStyle(this), TextBlock.GetFontWeight(this), TextBlock.GetFontStretch(this)),
					TextBlock.GetFontSize(this), Brushes.LightGray, 1)
				{
					MaxTextWidth = ActualWidth,
					MaxTextHeight = ActualHeight,
					TextAlignment = TextAlignment.Center,
					Trimming = TextTrimming.WordEllipsis
				};

				drawingContext.DrawText(text, new Point(0, centrePoint.Y - (text.Height / 2)));
			}
		}
	}
}
