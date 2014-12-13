using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Data;
using System.Windows.Documents;
using System.Windows.Input;
using System.Windows.Media;
using System.Windows.Media.Imaging;
using System.Windows.Navigation;
using System.Windows.Shapes;

namespace workflow
{
    /// <summary>
    /// 按照步骤 1a 或 1b 操作，然后执行步骤 2 以在 XAML 文件中使用此自定义控件。
    ///
    /// 步骤 1a) 在当前项目中存在的 XAML 文件中使用该自定义控件。
    /// 将此 XmlNamespace 特性添加到要使用该特性的标记文件的根 
    /// 元素中: 
    ///
    ///     xmlns:MyNamespace="clr-namespace:workflow"
    ///
    ///
    /// 步骤 1b) 在其他项目中存在的 XAML 文件中使用该自定义控件。
    /// 将此 XmlNamespace 特性添加到要使用该特性的标记文件的根 
    /// 元素中: 
    ///
    ///     xmlns:MyNamespace="clr-namespace:workflow;assembly=workflow"
    ///
    /// 您还需要添加一个从 XAML 文件所在的项目到此项目的项目引用，
    /// 并重新生成以避免编译错误: 
    ///
    ///     在解决方案资源管理器中右击目标项目，然后依次单击
    ///     “添加引用”->“项目”->[浏览查找并选择此项目]
    ///
    ///
    /// 步骤 2)
    /// 继续操作并在 XAML 文件中使用控件。
    ///
    ///     <MyNamespace:Polygen/>
    ///
    /// </summary>
    public class Polygen : Control
    {
        private Point start;
        public Point Start
        {
            get { return start; }
            set
            {
                if (this.start != value)
                {
                    this.start = value;
                    OnPropertyChanged("Start");
                }
            }
        }

        public static DependencyProperty StartProperty = DependencyProperty.RegisterAttached(
           "Start", typeof(Point), typeof(Polygen), new PropertyMetadata(new PropertyChangedCallback(OnStartPropertyChanged)));
        public static Point GetStartProperty(FrameworkElement ui)
        {
            return (Point)ui.GetValue(StartProperty);
        }
        public static void SetStartProperty(FrameworkElement ui, Point value)
        {
            ui.SetValue(StartProperty, value);
        }
        private static void OnStartPropertyChanged(DependencyObject d, DependencyPropertyChangedEventArgs e)
        {
            Polygen pol = d as Polygen;
            
            pol.OnPropertyChanged("Start");
        }

        public event PropertyChangedEventHandler PropertyChanged;
        protected void OnPropertyChanged(string name)
        {
            if (PropertyChanged != null)
            {
                PropertyChanged(this, new PropertyChangedEventArgs(name));
            }
        }




        private Point end;
        public Point End
        {
            get { return end; }
            set
            {
                if (this.end != value)
                {
                    this.end = value;
                    OnPropertyChanged("End");
                }
            }
        }

        public static DependencyProperty EndProperty = DependencyProperty.RegisterAttached(
           "End", typeof(Point), typeof(Polygen), new PropertyMetadata(new PropertyChangedCallback(OnEndPropertyChanged)));
        public static Point GetEndProperty(FrameworkElement ui)
        {
            return (Point)ui.GetValue(EndProperty);
        }
        public static void SetEndProperty(FrameworkElement ui, Point value)
        {
            ui.SetValue(EndProperty, value);
        }
        private static void OnEndPropertyChanged(DependencyObject d, DependencyPropertyChangedEventArgs e)
        {
            Polygen pol = d as Polygen;
            pol.OnPropertyChanged("End");
        }



        Grid partMain;

        static Polygen()
        {
            DefaultStyleKeyProperty.OverrideMetadata(typeof(Polygen), new FrameworkPropertyMetadata(typeof(Polygen)));
        }

        public override void OnApplyTemplate()
        {
            base.OnApplyTemplate();

            this.partMain = GetTemplateChild("PART_MAIN") as Grid;
        }

        protected override Size ArrangeOverride(Size arrangeBounds)
        {
            partMain.Children.Clear();

            //从开始点画竖折线
            Line topLine = new Line()
            {
                X1 = start.X,
                X2 = start.X,
                Y1 = start.Y,
                Y2 = end.Y,
                Stroke = new SolidColorBrush(Colors.Black),
                StrokeThickness = 1
            };
            //从竖折线结束到结束点画横线
            Line leftLine = new Line()
            {
                X1 = start.X,
                X2 = end.X,
                Y1 = end.Y,
                Y2 = end.Y,
                Stroke = new SolidColorBrush(Colors.Black),
                StrokeThickness = 1
            };
            //结束点箭头上
            Line arrorLine1 = new Line()
            {
                X1 = end.X,
                X2 = end.X-5,
                Y1 = end.Y,
                Y2 = end.Y-5,
                Stroke = new SolidColorBrush(Colors.Black),
                StrokeThickness = 1
            };
            //结束点箭头上
            Line arrorLine2 = new Line()
            {
                X1 = end.X,
                X2 = end.X - 5,
                Y1 = end.Y,
                Y2 = end.Y + 5,
                Stroke = new SolidColorBrush(Colors.Black),
                StrokeThickness = 1
            };
            partMain.Children.Add(topLine);
            partMain.Children.Add(leftLine);
            partMain.Children.Add(arrorLine1);
            partMain.Children.Add(arrorLine2);

            return base.ArrangeOverride(arrangeBounds);
        }
    }
}
