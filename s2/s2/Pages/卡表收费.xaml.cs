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
using System.Linq;
using System.Net;
using System.Json;

namespace Com.Aote.Pages
{
    public partial class 卡表收费 : UserControl
    {
        PagedList listwh = new PagedList();
        public 卡表收费()
        {
            InitializeComponent();
        }
        string userid = "";
        private void NewGeneralICCard_Completed(object sender, System.ComponentModel.AsyncCompletedEventArgs e)
        {

            busy.IsBusy = true;
            NewGeneralICCard card = (from p in loader.Res where p.Name.Equals("card") select p).First() as NewGeneralICCard;
            if (card.State == State.LoadError)
            {
                //WebClientInfo wci = Application.Current.Resources["server"] as WebClientInfo;
                //string str = wci.BaseAddress + "returns" + f_userid.Text;
                //Uri uri = new Uri(str);
                // WebClient client = new WebClient();
                //   client.DownloadStringCompleted += client_DownloadStringCompleted;
                //client.DownloadStringAsync(uri);
                MessageBox.Show("写卡失败,请读卡核对气量!");
          //  }
          //  else
          //  {
            //    print.Print();

            }
            busy.IsBusy = false;
        }
        private void ui_pregas_LostFocus(object sender, RoutedEventArgs e)
        {
            ui_chargeBusy.IsBusy = true;
            string userid = f_userid.Text;
            string pregas = ui_pregas.Text;
            if (userid.Equals(""))
            {
                MessageBox.Show("请先读卡！");
                ui_chargeBusy.IsBusy = false;
                return;
            }
            else if (pregas.Equals(""))
            {
                MessageBox.Show("请输入预购气量！");
                ui_chargeBusy.IsBusy = false;
                return;
            }
            WebClientInfo wci = (WebClientInfo)Application.Current.Resources["chargeserver"];
            string str = wci.BaseAddress + "/num/" + userid + "/" + pregas;
            Uri uri = new Uri(str);
            WebClient client = new WebClient();
            client.DownloadStringCompleted += client_DownloadStringCompleted;
            client.DownloadStringAsync(uri);

        }

        private void client_DownloadStringCompleted(object sender, DownloadStringCompletedEventArgs e)
        {
            ui_chargeBusy.IsBusy = false;
            if (e.Error == null)
            {
                JsonObject items = JsonValue.Parse(e.Result) as JsonObject;
                ui_stair1amont.Text = items["f_stair1amount"].ToString();
                ui_stair2amont.Text = items["f_stair2amount"].ToString();
                ui_stair3amont.Text = items["f_stair3amount"].ToString();
                ui_stair4amont.Text = items["f_stair4amount"].ToString();
                ui_stair1fee.Text = items["f_stair1fee"].ToString();
                ui_stair2fee.Text = items["f_stair2fee"].ToString();
                ui_stair3fee.Text = items["f_stair3fee"].ToString();
                ui_stair4fee.Text = items["f_stair4fee"].ToString();
                ui_stair1price.Text = items["f_stair1price"].ToString();
                ui_stair2price.Text = items["f_stair2price"].ToString();
                ui_stair3price.Text = items["f_stair3price"].ToString();
                ui_stair4price.Text = items["f_stair4price"].ToString();
                ui_allamont.Text = items["f_allamont"].ToString();
                if (items["f_stardate"] == null) {
                    items["f_stardate"] = "2050-12-12";
                }
                if (items["f_enddate"] == null)
                {
                    items["f_enddate"] = "2050-12-12";
                }
                ui_stardate.Text = items["f_stardate"].ToString().Substring(1, 10);
                ui_enddate.Text = items["f_enddate"].ToString().Substring(1, 10);
                ui_grossproceeds.Text = items["f_totalcost"].ToString();
                ui_preamount.Text = items["f_chargenum"].ToString();
                ui_totalcost.Text = items["f_totalcost"].ToString();
            }
            else
            {
                ui_chargeBusy.IsBusy = false;
                MessageBox.Show(e.Error.Message);
            }
        }

        private void ui_grossproceeds_LostFocus(object sender, RoutedEventArgs e)
        {
            ui_chargeBusy.IsBusy = true;
            string grossproceeds = ui_grossproceeds.Text;
            if (f_userid.Text.Equals(""))
            {
                MessageBox.Show("请先读卡！");
                ui_chargeBusy.IsBusy = false;
                return;
            }
            else if (grossproceeds.Equals(""))
            {
                MessageBox.Show("请输入预购金额！");
                ui_chargeBusy.IsBusy = false;
                return;
            }
            userid = f_userid.Text;
            WebClientInfo wci = (WebClientInfo)Application.Current.Resources["chargeserver"];
            string str = wci.BaseAddress + "/fee/" + userid + "/" + grossproceeds;
            Uri uri = new Uri(str);
            WebClient client1 = new WebClient();
            client1.DownloadStringCompleted += client1_DownloadStringCompleted;
            client1.DownloadStringAsync(uri);
        }
        double pregas = 0;
        private void client1_DownloadStringCompleted(object sender, DownloadStringCompletedEventArgs e)
        {
            ui_chargeBusy.IsBusy = false;
            if (e.Error == null)
            {
                JsonObject items = JsonValue.Parse(e.Result) as JsonObject;
                pregas = Math.Floor(double.Parse(items["chargeamont"].ToString()));
                WebClientInfo wci = (WebClientInfo)Application.Current.Resources["chargeserver"];
                string str = wci.BaseAddress + "/num/" + userid + "/" + pregas;
                Uri uri = new Uri(str);
                WebClient client2 = new WebClient();
                client2.DownloadStringCompleted += client2_DownloadStringCompleted;
                client2.DownloadStringAsync(uri);

            }
            else
            {
                ui_chargeBusy.IsBusy = false;
                MessageBox.Show(e.Error.Message);
            }
        }

        private void client2_DownloadStringCompleted(object sender, DownloadStringCompletedEventArgs e)
        {
            ui_chargeBusy.IsBusy = false;
            if (e.Error == null)
            {
                JsonObject items = JsonValue.Parse(e.Result) as JsonObject;
                ui_stair1amont.Text = items["f_stair1amount"].ToString();
                ui_stair2amont.Text = items["f_stair2amount"].ToString();
                ui_stair3amont.Text = items["f_stair3amount"].ToString();
                ui_stair4amont.Text = items["f_stair4amount"].ToString();
                ui_stair1fee.Text = items["f_stair1fee"].ToString();
                ui_stair2fee.Text = items["f_stair2fee"].ToString();
                ui_stair3fee.Text = items["f_stair3fee"].ToString();
                ui_stair4fee.Text = items["f_stair4fee"].ToString();
                ui_stair1price.Text = items["f_stair1price"].ToString();
                ui_stair2price.Text = items["f_stair2price"].ToString();
                ui_stair3price.Text = items["f_stair3price"].ToString();
                ui_stair4price.Text = items["f_stair4price"].ToString();
                ui_allamont.Text = items["f_allamont"].ToString();
                if (items["f_stardate"] == null)
                {
                    items["f_stardate"] = "2050-12-12";
                }
                if (items["f_enddate"] == null)
                {
                    items["f_enddate"] = "2050-12-12";
                }
                ui_stardate.Text = items["f_stardate"].ToString().Substring(1, 10);
                ui_enddate.Text = items["f_enddate"].ToString().Substring(1, 10);
                ui_preamount.Text = items["f_chargenum"].ToString();
                ui_totalcost.Text = Math.Round(double.Parse(items["f_totalcost"].ToString()), 2).ToString();
                ui_pregas.Text = pregas.ToString();

            }
            else
            {
                ui_chargeBusy.IsBusy = false;
                MessageBox.Show(e.Error.Message);
            }
        }

        private void DisplayPopup(object sender, RoutedEventArgs e)
        {
            if (myPopup.IsOpen == false) { 
            myPopup.IsOpen = true;
            }
            else{
                myPopup.IsOpen = false;
            }
        }


    }
}