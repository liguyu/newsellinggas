using Com.Aote.ObjectTools;
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

namespace Com.Aote.Pages
{
    public partial class 换表信息查询 : UserControl
    {
        // search对象
        SearchObject userSearch = new SearchObject();

        // 选中的表格对象
        PagedList userList = new PagedList();
        public 换表信息查询()
        {
            InitializeComponent();
            // 给界面上的search部分，设置数据
            ui_setSearch.DataContext = userSearch;

            // 给两个表格对象挂接数据
            ui_gqdDataGrid.ItemsSource = userList;
            userList.DataLoaded += setList_DataLoaded;
        }

        private void setList_DataLoaded(object sender, System.ComponentModel.AsyncCompletedEventArgs e)
        {
            ui_userBusy.IsBusy = false;
            
        }
        string sql = "";
        int pageIndex = 0;
        private void ui_SearchButton_Click(object sender, RoutedEventArgs e)
        {
            
            ui_userBusy.IsBusy = true;
            userSearch.Search();
            
            // 生成sql语句
            sql = " from t_changmeter where " + userSearch.Condition+" order by id";
            userList.WebClientInfo = Application.Current.Resources["dbclient"] as WebClientInfo;
            userList.LoadOnPathChanged = false;
            userList.Path = "hql";
            userList.SumHQL = " from t_changmeter where " + userSearch.Condition;
            userList.HQL = sql;
            userList.PageSize = ui_pager.PageSize;
            userList.SumNames = "id";
            userList.PageIndex = pageIndex;
            userList.Load();
        }

        private void ui_pager_PageIndexChanged(object sender, EventArgs e)
        {
            userList.PageIndex = ui_pager.PageIndex;
        }
    }
}
