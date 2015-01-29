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
using Com.Aote.Controls;

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

        private void zhikong_Click(object sender, RoutedEventArgs e)
        {
            ui_address.Text  = "";
            ui_userid.Text   = "";
            ui_username.Text = "";
            ui_whetherback.SelectedValue = "";
        }

        private void excel_Click(object sender, RoutedEventArgs e)
        {
            ui_userBusy.IsBusy = true;
            int pagecount = Convert.ToInt32(ui_pagedcount.Text);
            WebClientInfo wcf = Application.Current.Resources["server"] as WebClientInfo;
            string url = wcf.BaseAddress + "/excel/" + pagecount + "/id:编号|f_username:姓名|f_userid:用户编号|f_address:地址|f_usertype:用户类型|f_watchfee:表费|f_cardfees:卡费|f_cmnewgaswatchbrand:新表品牌|f_cmnewmetertype:新表型号|f_cmnewmeternumber:新表表号|f_cmaddgas:补气量|f_cypregas:超用原因|f_cnote:换表原因|f_cmoperator:换表人|f_cmdate:换表日期|f_cancelnote:撤销原因|f_cxoperation:撤销人|f_canceldate:撤销日期";
            toExcel.HQL = " from t_changmeter where " + userSearch.Condition + " order by id";
            toExcel.Path = url;
            toExcel.ToExcel();
            toExcel.Completed += toExcel_Completed;
        }

        void toExcel_Completed(object sender, EventArgs e)
        {
            WebClientInfo wcf = Application.Current.Resources["server"] as WebClientInfo;
            string uri = wcf.BaseAddress + "/file/" + toExcel.FileName;
            downLoad.Path = uri;
            downLoad.Completed += downLoad_Completed;
            downLoad.Down();
        }

        void downLoad_Completed(object sender, EventArgs e)
        {
            ui_userBusy.IsBusy = false;
            excelmessage.Show();
        }
    }
}
