using System;
using System.Collections.Generic;
using System.Linq;
using System.Net;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Documents;
using System.Windows.Input;
using System.Windows.Media;
using System.Windows.Media.Animation;
using System.Windows.Shapes;
using System.Windows.Browser;
using Com.Aote.ObjectTools;
using Com.Aote.Utils;

namespace Com.Aote.Pages
{
    public partial class 来电管理 : UserControl
    {
        public 来电管理()
        {
            InitializeComponent();
        }

        private void jiedanren_MouseEnter(object sender, MouseEventArgs e)
        {
            jiedanrenlist.Load();
        }

    }
}