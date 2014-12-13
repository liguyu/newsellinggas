using System;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Documents;
using System.Windows.Ink;
using System.Windows.Input;
using System.Windows.Media;
using System.Windows.Media.Animation;
using System.Windows.Shapes;
using Com.Aote.ObjectTools;

namespace Com.Aote.Pages
{
	public partial class 稽查生成 : UserControl
	{
        SearchObject search = new SearchObject();
       PagedList list = new PagedList();
		public 稽查生成()
		{
          
			// Required to initialize variables
			InitializeComponent();
            daninfosearch.DataContext = search;
            daninfos.ItemsSource = list;
            list.DataLoaded += list_DataLoaded;
		}
        int PageIndex = 0;
   
		private void dansearchbutton_Click(object sender, System.Windows.RoutedEventArgs e)
		{
            searchbusy.IsBusy = true;
            search.Search();

			// TODO: Add event handler implementation here.
            string sql = "select u.f_userid,u.f_username,u.f_cardid,u.f_gaswatchbrand,u.f_metertype,u.f_address,u.f_districtname,u.f_apartment " +
",u.f_phone,u.f_cumulativepurchase,u.f_metergasnums,CONVERT(varchar(100),u.f_dateofopening,23) f_dateofopening, CONVERT(varchar(100),d.f_deliverydate,23) f_deliverydate,d.f_pregas from (" +
"select s.f_userid,t.f_deliverydate,t.f_pregas from" +
"(select MAX(id) m,f_userid from t_sellinggas where f_payfeevalid = '有效' group by f_userid)s  left join t_sellinggas t on s.m = t.id)d left join t_userfiles u on d.f_userid = u.f_userid where f_gasmeterstyle = '卡表' and " + search.Condition + " order by u.f_userid";
            list.WebClientInfo = Application.Current.Resources["dbclient"] as WebClientInfo;
            list.LoadOnPathChanged = false;
            list.Path = "sql";
            list.HQL = sql;
            list.SumHQL = "select u.f_userid,u.f_username,u.f_cardid,u.f_gaswatchbrand,u.f_metertype,u.f_address,u.f_districtname,u.f_apartment " +
",u.f_phone,u.f_cumulativepurchase,u.f_metergasnums,u.f_dateofopening,d.f_deliverydate,d.f_pregas from (" +
"select s.f_userid,t.f_deliverydate,t.f_pregas from" +
"(select MAX(id) m,f_userid from t_sellinggas where f_payfeevalid = '有效' group by f_userid)s  left join t_sellinggas t on s.m = t.id)d left join t_userfiles u on d.f_userid = u.f_userid where f_gasmeterstyle = '卡表' and " + search.Condition + "";
            list.SumNames = ",";
            list.PageSize = pager.PageSize ;
            list.PageIndex = PageIndex;
            list.Load();
		}
        private void ui_pager(object sender, EventArgs e)
        {
            list.PageIndex = pager.PageIndex;
        }
        private void list_DataLoaded(object sender, System.ComponentModel.AsyncCompletedEventArgs e)
        {
            searchbusy.IsBusy = false;
        }

        private void Button_Click(object sender, System.Windows.RoutedEventArgs e)
        {
        	// TODO: Add event handler implementation here.
			searchbusy.IsBusy = true;
            int pagecount = Convert.ToInt32(daninfoscount.Text);
            WebClientInfo wci = Application.Current.Resources["server"] as WebClientInfo;
            string uri = wci.BaseAddress + "/excel/" + pagecount + "/f_userid:用户编号|f_username:姓名|f_cardid:卡号|f_metertype:气表型号|f_address:地址|f_districtname:小区名称|f_apartment:门牌号|f_phone:电话|f_cumulativepurchase:总购气量|f_metergasnums:当前表购气量|f_dateofopening:开户日期|f_pregas:最后购气量|f_deliverydate:最后购气日期";
            toExcel.HQL = "sql:select u.f_userid,u.f_username,u.f_cardid,u.f_gaswatchbrand,u.f_metertype,u.f_address,u.f_districtname,u.f_apartment " +
",u.f_phone,u.f_cumulativepurchase,u.f_metergasnums,u.f_dateofopening,d.f_pregas,d.f_deliverydate from (" +
"select s.f_userid,t.f_deliverydate,t.f_pregas from" +
"(select MAX(id) m,f_userid from t_sellinggas where f_payfeevalid = '有效' group by f_userid)s  left join t_sellinggas t on s.m = t.id)d left join t_userfiles u on d.f_userid = u.f_userid where f_gasmeterstyle = '卡表' and " + search.Condition + "";
            toExcel.Path = uri;
            toExcel.Completed += toExcel_Completed;
            toExcel.ToExcel();
        }
        private void toExcel_Completed(object sender,EventArgs e)
        {
            WebClientInfo wc = Application.Current.Resources["server"] as WebClientInfo;
            string uro = wc.BaseAddress + "/file/" + toExcel.FileName;
            downLoad.Path = uro;
            downLoad.Completed += download_Completed;
            downLoad.Down();
        }


        private void download_Completed(object sender,EventArgs e) 
        {
            searchbusy.IsBusy = false;
            toExcelMessger.Show();
        }
    }
}