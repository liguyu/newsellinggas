using System;
using System.Collections.Generic;
using System.Json;
using System.Linq;
using System.Net;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Documents;
using System.Windows.Input;
using System.Windows.Media;
using System.Windows.Media.Animation;
using System.Windows.Shapes;
using Com.Aote.ObjectTools;

namespace s2.Pages
{
    public partial class 燃气表故障明细 : UserControl
    {
        public 燃气表故障明细()
        {
            InitializeComponent();
        }

        private void btnSearch_Click(object sender, RoutedEventArgs e)
        {
            SearchObject conditions = (criteriaPanel.DataContext as SearchObject);
            conditions.Search();
			WebClientInfo wci = App.Current.Resources["dbclient"] as WebClientInfo;
            String dt = "1=1";
            if (StartDate.Text.Trim().Length != 0)
                dt = " DEPARTURE_TIME>='" + StartDate.Text + "'";
            if (EndDate.Text.Trim().Length != 0)
            {
                if (dt.Length > 3)
                    dt += " and DEPARTURE_TIME<='" + EndDate.Text + " 23:59:59'";
                else
                    dt = " DEPARTURE_TIME<='" + EndDate.Text + " 23:59:59' ";
            }

            checkerList.LoadOnPathChanged = false;
            checkerList.Path = "sql";
            checkerList.Names = "id,CONDITION,UNIT_NAME,CUS_DOM,CUS_DY,CUS_FLOOR,CUS_ROOM,USER_NAME,CARD_ID,TELPHONE,SAVE_PEOPLE,DEPARTURE_TIME,ARRIVAL_TIME,RQB_AROUND,REPAIRMAN,CONTENT,sn";
            String sql = @"select t.id,t.CONDITION,t.UNIT_NAME,t.CUS_DOM,t.CUS_DY,t.CUS_FLOOR,t.CUS_ROOM,t.USER_NAME,t.CARD_ID,t.TELPHONE,t.SAVE_PEOPLE,t.DEPARTURE_TIME,t.ARRIVAL_TIME,t.RQB_AROUND,t.REPAIRMAN,tt.CONTENT from T_INSPECTION t , T_INSPECTION_LINE tt where t.id=tt.inspection_id  
 and (t.DELETED is null or t.DELETED <> '是') and tt.EQUIPMENT='燃气表' and tt.CONTENT in ('表不过气', '长通表', '死表') and {0} and {1}";
            checkerList.HQL = String.Format(sql, new String[]{conditions.Condition, dt}).Replace("\r\n", " ");
            checkerList.Load();
        }


        private void picture_MouseEnter(object sender, MouseEventArgs e)
        {
            Image image = sender as Image;
            if (image.Source == null)
                return;
            bigPic.Source = image.Source;
        }

        /// <summary>
        /// 安检单列表选择改变
        /// </summary>
        /// <param name="sender"></param>
        /// <param name="e"></param>
        private void checkerGrid_SelectionChanged(object sender, SelectionChangedEventArgs e)
        {
            GeneralObject item = checkerGrid.SelectedItem as GeneralObject;
            if (item == null)
                return;
            GeneralObject go = new GeneralObject();
            go.WebClientInfo = Application.Current.Resources["dbclient"] as WebClientInfo;
            go.EntityType = "T_INSPECTION";
            if (!item.GetPropertyValue("CONDITION").Equals("未检"))
            {
                go.Path = "one/select distinct t from T_INSPECTION t left join fetch t.LINES where t.id ='" + item.GetPropertyValue("id").ToString() + "'";
                go.DataLoaded += go_DataLoaded;
                //if (!item.GetPropertyValue("CONDITION").Equals("正常"))
                //    userfile.IsEnabled = false;
                //else
                //    userfile.IsEnabled = true;
                go.Load();
            }
            else
            {
                userfile.DataContext = null;
                ClearAll();
                userfile.IsEnabled = false;
            }
        }

        void go_DataLoaded(object sender, System.ComponentModel.AsyncCompletedEventArgs e)
        {
            (sender as GeneralObject).DataLoaded -= go_DataLoaded;
            transformData(sender as GeneralObject);
            userfile.DataContext = sender;
        }

        private void transformData(GeneralObject go)
        {
            ClearAll();

            if (go == null)
                return;

            PostUITask(go);
        }

        private void ClearAll()
        {
            //清除以前的选择
            ClearChecks();
            bigPic.Source = null;
            CurrentArchiveAddress.Text = "";
            WARM_OTHER.Text = "";
            JB_METER_NAME_OTHER.Text = "";
            IC_METER_NAME_OTHER.Text = "";
            WARM.SelectedItem = null;
            IC_METER_NAME.SelectedItem = null;
            JB_METER_NAME.SelectedItem = null;
        }

        private void ClearChecks()
        {
            Panel[] panels = { MeterDefectsPane,  PlumbingDefectsPane, PlumbingProofPane, PlumbingPressurePane, PlumbingMeterValvePane, PlumbingCookerValvePane, PlumbingAutomaticValvePane, PlumbingPipePane, PlumbingLeakagePane, 
                                 CookerPipePane, BoilerPipePane, BoilerDefectsPane, WHEDefectsPane, precautionCheckPane };
            foreach (Panel p in panels)
            {
                foreach (UIElement element in p.Children)
                {
                    if (element is CheckBox && !(element as CheckBox).Content.ToString().Equals("正常") && !(element as CheckBox).Content.ToString().Equals("无"))
                    {
                        if (element == cbRIGIDITYLeakage || element == cbRIGIDITYNormal || element == cbPressureNormal || element == cbPressureAbnormal ||
                            element == cbLEAKAGE_COOKER || element == cbLEAKAGE_BOILER || element == cbLEAKAGE_HEATER || element == cbLEAKAGE_NOTIFIED)
                            continue;
                        (element as CheckBox).IsChecked = false;
                    }
                    if (element is RadioButton)
                        (element as RadioButton).IsChecked = false;
                }
            }
        }


        private void PostUITask(GeneralObject go)
        {
            if (go.GetPropertyValue("CONDITION").ToString().Equals("正常"))
            {
                //供暖方式
                ObjectCollection oc = this.Resources["WARM"] as ObjectCollection;
                bool found = false;
                foreach (Pair pair in oc)
                {
                    if (pair.CName.Equals(go.GetPropertyValue("WARM").ToString()))
                    {
                        found = true;
                        WARM.SelectedItem = pair;
                    }
                }
                if (!found)
                {
                    WARM_OTHER.Text = go.GetPropertyValue("WARM").ToString();
                    WARM.SelectedIndex = oc.Count - 1;
                }

                //基表厂家型号
                oc = this.Resources["JB_METER_NAME"] as ObjectCollection;
                found = false;
                foreach (Pair pair in oc)
                {
                    if (pair.CName.Equals(go.GetPropertyValue("JB_METER_NAME").ToString()))
                    {
                        JB_METER_NAME.SelectedItem = pair;
                        found = true;
                    }
                }
                if (!found)
                {
                    JB_METER_NAME_OTHER.Text = go.GetPropertyValue("JB_METER_NAME").ToString();
                    JB_METER_NAME.SelectedIndex = oc.Count - 1;
                }

                //IC卡表厂家型号
                oc = this.Resources["IC_METER_NAME"] as ObjectCollection;
                found = false;
                foreach (Pair pair in oc)
                {
                    if (pair.CName.Equals(go.GetPropertyValue("IC_METER_NAME").ToString()))
                    {
                        found = true;
                        IC_METER_NAME.SelectedItem = pair;
                    }
                }
                if (!found)
                {
                    IC_METER_NAME_OTHER.Text = go.GetPropertyValue("IC_METER_NAME").ToString();
                    go.SetPropertyValue("IC_METER_NAME", (oc.ElementAt(oc.Count - 1) as Pair).Code, true, true);
                    IC_METER_NAME.SelectedIndex = oc.Count - 1;
                }

                ObjectList lines = go.GetPropertyValue("LINES") as ObjectList;
                //不存在隐患
                if (lines == null)
                    return;

                foreach (GeneralObject line in lines)
                {
                    String EQUIPMENT = line.GetPropertyValue("EQUIPMENT") as string;
                    String CONTENT = line.GetPropertyValue("CONTENT") as string;
                    if (EQUIPMENT.Equals("安全隐患"))
                        CheckCheckBox(CONTENT, precautionCheckPane);
                    else if (EQUIPMENT.Equals("燃气表"))
                        CheckCheckBox(CONTENT, MeterDefectsPane);
                    else if (EQUIPMENT.Equals("立管"))
                        CheckPlumbingBox(CONTENT, PlumbingDefectsPane);
                    else if (EQUIPMENT.Equals("阀门表前阀"))
                        CheckCheckBox(CONTENT, PlumbingMeterValvePane);
                    else if (EQUIPMENT.Equals("阀门灶前阀"))
                        CheckCheckBox(CONTENT, PlumbingCookerValvePane);
                    else if (EQUIPMENT.Equals("阀门自闭阀"))
                        CheckCheckBox(CONTENT, PlumbingAutomaticValvePane);
                    else if (EQUIPMENT.Equals("户内管"))
                        CheckCheckBox(CONTENT, PlumbingPipePane);
                    else if (EQUIPMENT.Equals("灶具软管"))
                        CheckCheckBox(CONTENT, CookerPipePane);
                    else if (EQUIPMENT.Equals("热水器软管"))
                        CheckCheckBox(CONTENT, BoilerPipePane);
                    else if (EQUIPMENT.Equals("热水器安全隐患"))
                        CheckCheckBox(CONTENT, BoilerDefectsPane);
                    else if (EQUIPMENT.Equals("壁挂锅炉安全隐患"))
                        CheckCheckBox(CONTENT, WHEDefectsPane);
                }

                //提取用户档案地址
                String card_id = go.GetPropertyValue("CARD_ID") as string;
                if(IsNullOrEmpty(card_id))
                    return;
                WebClient wc = new WebClient();
                wc.DownloadStringCompleted += wc_GetUserProfileCompleted;
                wc.DownloadStringAsync(new Uri(go.WebClientInfo.BaseAddress + "/one/from T_IC_USERFILE where CARD_ID='" + card_id +"'"));
            }
            
        }

        private void wc_GetUserProfileCompleted(object sender, DownloadStringCompletedEventArgs e)
        {
            if (e.Error == null)
            {
                JsonObject item = JsonValue.Parse(e.Result) as JsonObject;
                String ROAD = null;
                String UNIT_NAME = null;
                String CUS_DOM = null;
                String CUS_DY = null;
                String CUS_FLOOR = null;
                String CUS_ROOM = null;
                if (item.ContainsKey("ROAD"))
                    ROAD = item["ROAD"];
                if (item.ContainsKey("UNIT_NAME"))
                    UNIT_NAME = item["UNIT_NAME"];
                if (item.ContainsKey("CUS_DOM"))
                    CUS_DOM = item["CUS_DOM"];
                if (item.ContainsKey("CUS_DY"))
                    CUS_DY = item["CUS_DY"];
                if (item.ContainsKey("CUS_FLOOR"))
                    CUS_FLOOR = item["CUS_FLOOR"];
                if (item.ContainsKey("CUS_ROOM"))
                    CUS_ROOM = item["CUS_ROOM"];
                CurrentArchiveAddress.Text = String.Format("{0}\t{1}\t{2}-{3}-{4}-{5}",
                    new String[] { ROAD, UNIT_NAME, CUS_DOM,CUS_DY, CUS_FLOOR, CUS_ROOM});                
            }
        }

        private void CheckPlumbingBox(String CONTENT, Panel panel)
        {
            foreach (UIElement element in panel.Children)
            {
                if (element is CheckBox)
                {
                    CheckBox aBox = element as CheckBox;
                    if (aBox.Content.ToString().Equals(CONTENT))
                        aBox.IsChecked = true;
                }
                if (element is RadioButton)
                {
                    RadioButton aBox = element as RadioButton;
                    if (aBox.Content.ToString().Equals(CONTENT))
                    {
                        aBox.IsChecked = true;
                        cbEroded.IsChecked = true;
                    }
                }
            }
        }

        private void CheckCheckBox(String CONTENT, Panel panel)
        {
            foreach (UIElement element in panel.Children)
            {
                if (element is CheckBox)
                {
                    CheckBox aBox = element as CheckBox;
                    if (aBox.Content.ToString().Equals(CONTENT))
                        aBox.IsChecked = true;
                }
            }
        }

        private Boolean IsNullOrEmpty(String value)
        {
            return value == null || value.Trim().Length == 0;
        }

        /*private void outputButton_Click(object sender, System.Windows.RoutedEventArgs e)
        {
        	// TODO: Add event handler implementation here.
			SearchObject conditions = (criteriaPanel.DataContext as SearchObject);
            conditions.Search();
			WebClientInfo wci = App.Current.Resources["dbclient"] as WebClientInfo;
            String dt = " t.DEPARTURE_TIME>='" + StartDate.Text + "' and t.DEPARTURE_TIME<='" + EndDate.Text + " 23:59:59' ";
            checkerList.LoadOnPathChanged = false;
            checkerList.Path = "sql";
            checkerList.Names = "id,CONDITION,UNIT_NAME,CUS_DOM,CUS_DY,CUS_FLOOR,CUS_ROOM,USER_NAME,CARD_ID,TELPHONE,SAVE_PEOPLE,DEPARTURE_TIME,ARRIVAL_TIME,RQB_AROUND,REPAIRMAN,CONTENT,sn";
            String sql = @"select t.id,t.CONDITION,t.UNIT_NAME,t.CUS_DOM,t.CUS_DY,t.CUS_FLOOR,t.CUS_ROOM,t.USER_NAME,t.CARD_ID,t.TELPHONE,t.SAVE_PEOPLE,t.DEPARTURE_TIME,t.ARRIVAL_TIME,t.RQB_AROUND,t.REPAIRMAN,tt.CONTENT from T_INSPECTION t , T_INSPECTION_LINE tt where t.id=tt.inspection_id  
 and (t.DELETED is null or t.DELETED <> '是') and tt.EQUIPMENT='燃气表' and tt.CONTENT in ('表不过气', '长通表', '死表') and {0} and {1}";
            checkerList.HQL = String.Format(sql, new String[]{conditions.Condition, dt}).Replace("\r\n", " ");
			toExcel.HQL = checkerList.HQL;
            toExcel.Completed += new EventHandler(toExcel_Completed);
            toExcel.Path = wci.BaseAddress + "/excel/" + (checkerGrid.ItemsSource as ObjectList).Count + "/UNIT_NAME:小区|CUS_DOM:楼号|CUS_DY:单元|CUS_FLOOR:楼层|CUS_ROOM:房号|USER_NAME:用户名|TELPHONE:用户电话|CARD_ID:卡号|SAVE_PEOPLE:安检员|DEPARTURE_TIME:安检时间|RQB_AROUND:表向|CONTENT:故障类型|REPAIRMAN:维修员";
            toExcel.ToExcel();
        }
		void toExcel_Completed(object sender, EventArgs e)
        {
            downLoad.Down();
        }*/

    }
}
