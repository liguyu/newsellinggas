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
using Com.Aote.Controls;

namespace s2.Pages
{
    public partial class 其他严重隐患类型明细表 : UserControl
    {
        public 其他严重隐患类型明细表()
        {
            InitializeComponent();
        }

        private void btnSearch_Click(object sender, RoutedEventArgs e)
        {
            /*SearchObject conditions = (criteriaPanel.DataContext as SearchObject);
            conditions.Search();
            WebClientInfo wci = App.Current.Resources["dbclient"] as WebClientInfo;
            WebClient wc = new WebClient();
            String precaution = "";
            if(cmbPrecaution.SelectedValue.ToString().Length>0)
                precaution = " where precaution ='" + cmbPrecaution.SelectedValue + "'";
            String dt = " and departure_time >='" + StartDate.Text + "' and departure_time <= '" + EndDate.Text + " 23:59:59' ";
            String HQL = @"select precaution, road, unit_name, cus_dom, cus_dy, cus_floor, cus_room, user_name, telphone, departure_time, precaution_notified, cast(row_number() over (partition by precaution order by precaution) as INTEGER) sn from
                            (select 
                            cast(case content when '燃气设备安装在卧室' then '燃气设施安装在卧室' 
                            when '燃气设施安装在卧室/卫生间' then '燃气设施安装在卧室' 
                            when '安装在卧室' then '热水器或壁挂炉安装在卧室' 
                            when '壁挂炉安装在卧室' then '热水器或壁挂炉安装在卧室' 
                            when '直排热水器' then '使用直排式热水器' 
                            else '热水器未安装烟道或烟道未接到室外' end as VARCHAR(50)) precaution, 
                            t.ROAD,t.UNIT_NAME, t.CUS_DOM, t.CUS_DY, t.CUS_FLOOR,t.CUS_ROOM, t.USER_NAME, t.TELPHONE, SUBSTRING(t.DEPARTURE_TIME,1,10) DEPARTURE_TIME, NVL(t.PRECAUTION_NOTIFIED,'') precaution_notified  
                            from T_INSPECTION t join T_INSPECTION_LINE tl on t.id = tl.INSPECTION_ID where 
                            CONTENT in ('燃气设备安装在卧室','燃气设施安装在卧室/卫生间','壁挂炉安装在卧室','安装在卧室','未安装到烟道', '烟道未排到室外', '直排热水器') and (t.DELETED is null or t.DELETED <> '是') and {0}
                            )  {1} order by precaution, road, unit_name, length(cus_dom), cus_dom, length(cus_dy), cus_dy, length(cus_floor), cus_floor, length(cus_room), cus_room ";
            HQL = String.Format(HQL, new string[]{conditions.Condition + dt, precaution }).Replace("\r\n", " ").Replace("\n", " ").Replace("\r", " ");
            wc.UploadStringCompleted += wc_UploadStringCompleted;
            wc.UploadStringAsync(new Uri(wci.BaseAddress + "/sql/precaution,road,unit_name,cus_dom,cus_dy,cus_floor,cus_room,user_name,telphone,departure_time,precaution_notified,sn/0/999999999"), "POST", HQL);*/
			SearchObject conditions = (criteriaPanel.DataContext as SearchObject);
            conditions.Search();
			WebClientInfo wci = App.Current.Resources["dbclient"] as WebClientInfo;
            String precaution = "";
            if(cmbPrecaution.SelectedValue.ToString().Length>0)
                precaution = " where precaution ='" + cmbPrecaution.SelectedValue + "'";
            String dt = " 1=1";
            if (StartDate.Text.Trim().Length != 0)
                dt = " DEPARTURE_TIME>='" + StartDate.Text + "'";
            if (EndDate.Text.Trim().Length != 0)
            {
                if (dt.Length > 3)
                    dt += " and DEPARTURE_TIME<='" + EndDate.Text + " 23:59:59'";
                else
                    dt = " DEPARTURE_TIME<='" + EndDate.Text + " 23:59:59' ";
            }
            dt = " and " + dt;
            checkerList.Path = "sql";
            checkerList.Names = "precaution,road,unit_name,cus_dom,cus_dy,cus_floor,cus_room,user_name,telphone,departure_time,precaution_notified,sn";
            String sql =@"select precaution, road, unit_name, cus_dom, cus_dy, cus_floor, cus_room, user_name, telphone, departure_time, precaution_notified, cast(row_number() over (partition by precaution order by precaution) as INTEGER) sn from
                            (select 
                            cast(case content when '燃气设备安装在卧室' then '燃气设施安装在卧室' 
                            when '燃气设施安装在卧室/卫生间' then '燃气设施安装在卧室' 
                            when '安装在卧室' then '热水器或壁挂炉安装在卧室' 
                            when '壁挂炉安装在卧室' then '热水器或壁挂炉安装在卧室' 
                            when '直排热水器' then '使用直排式热水器' 
                            else '热水器未安装烟道或烟道未接到室外' end as VARCHAR(50)) precaution, 
                            t.ROAD,t.UNIT_NAME, t.CUS_DOM, t.CUS_DY, t.CUS_FLOOR,t.CUS_ROOM, t.USER_NAME, t.TELPHONE, SUBSTRING(t.DEPARTURE_TIME,1,10) DEPARTURE_TIME, isnull(t.PRECAUTION_NOTIFIED,'') precaution_notified  
                            from T_INSPECTION t join T_INSPECTION_LINE tl on t.id = tl.INSPECTION_ID where 
                            CONTENT in ('燃气设备安装在卧室','燃气设施安装在卧室/卫生间','壁挂炉安装在卧室','安装在卧室','未安装到烟道', '烟道未排到室外', '直排热水器') and (t.DELETED is null or t.DELETED <> '是') and {0}
                            )  o {1}   order by precaution, road, unit_name, len(cus_dom), cus_dom, len(cus_dy), cus_dy, len(cus_floor), cus_floor, len(cus_room), cus_room ";
            checkerList.HQL = String.Format(sql, new string[]{conditions.Condition + dt, precaution }).Replace("\r\n", " ").Replace("\n", " ").Replace("\r", " ");
            checkerList.Load();
        }

        void wc_UploadStringCompleted(object sender, UploadStringCompletedEventArgs e)
        {
            if (e.Error == null)
            {
                JsonArray items = JsonValue.Parse(e.Result) as JsonArray;
                ObjectList list = new ObjectList();
                list.EntityType = "T_INSPECTION_LINE";
                foreach(JsonObject row in items)
                {
                    GeneralObject go = new GeneralObject();
                    go.EntityType = "T_INSPECTION_LINE";
                    go.SetPropertyValue("precaution", row["precaution"], true);
                    go.SetPropertyValue("road", row["road"], true);
                    go.SetPropertyValue("unit_name", row["unit_name"], true);
                    go.SetPropertyValue("cus_dom", row["cus_dom"], true);
                    go.SetPropertyValue("cus_dy", row["cus_dy"], true);
                    go.SetPropertyValue("cus_floor", row["cus_floor"], true);
                    go.SetPropertyValue("cus_room", row["cus_room"], true);
                    go.SetPropertyValue("user_name", row["user_name"], true);
                    go.SetPropertyValue("telphone", row["telphone"], true);
                    go.SetPropertyValue("departure_time", row["departure_time"], true);
                    go.SetPropertyValue("precaution_notified", row["precaution_notified"], true);
                    go.SetPropertyValue("sn", row["sn"], true);
                    list.Add(go);
                }
                paperGrid.ItemsSource = list;
            }
        }

        /*private void outputbutton_Click(object sender, System.Windows.RoutedEventArgs e)
        {
            SearchObject conditions = (criteriaPanel.DataContext as SearchObject);
            conditions.Search();
            WebClientInfo wci = App.Current.Resources["server"] as WebClientInfo;
            String precaution = "";
            if (cmbPrecaution.SelectedValue.ToString().Length > 0)
                precaution = " where precaution ='" + cmbPrecaution.SelectedValue + "'";
            String dt = " and departure_time >='" + StartDate.Text + "' and departure_time <= '" + EndDate.Text + " 23:59:59' ";
            String HQL = @"select precaution, road, unit_name, cus_dom, cus_dy, cus_floor, cus_room, user_name, telphone, departure_time, precaution_notified, cast(row_number() over (partition by precaution order by precaution) as INTEGER) sn from
                            (select 
                            cast(case content when '燃气设备安装在卧室' then '燃气设施安装在卧室' 
                            when '燃气设施安装在卧室/卫生间' then '燃气设施安装在卧室' 
                            when '安装在卧室' then '热水器或壁挂炉安装在卧室' 
                            when '壁挂炉安装在卧室' then '热水器或壁挂炉安装在卧室' 
                            when '直排热水器' then '使用直排式热水器' 
                            else '热水器未安装烟道或烟道未接到室外' end as VARCHAR(50)) precaution, 
                            t.ROAD,t.UNIT_NAME, t.CUS_DOM, t.CUS_DY, t.CUS_FLOOR,t.CUS_ROOM, t.USER_NAME, t.TELPHONE, SUBSTRING(t.DEPARTURE_TIME,1,10) DEPARTURE_TIME, NVL(t.PRECAUTION_NOTIFIED,'') precaution_notified  
                            from T_INSPECTION t join T_INSPECTION_LINE tl on t.id = tl.INSPECTION_ID where 
                            CONTENT in ('燃气设备安装在卧室','燃气设施安装在卧室/卫生间','壁挂炉安装在卧室','安装在卧室','未安装到烟道', '烟道未排到室外', '直排热水器') and (t.DELETED is null or t.DELETED <> '是') and {0}
                            )  {1} order by precaution, road, unit_name, length(cus_dom), cus_dom, length(cus_dy), cus_dy, length(cus_floor), cus_floor, length(cus_room), cus_room ";
            HQL = String.Format(HQL, new string[] { conditions.Condition + dt, precaution }).Replace("\r\n", " ").Replace("\n", " ").Replace("\r", " ");
            toExcel.HQL = "sql:{" + HQL + "}";
            toExcel.Completed += new EventHandler(toExcel_Completed);
            toExcel.Path = wci.BaseAddress + "/excel/" + (paperGrid.ItemsSource as ObjectList).Count + "/PRECAUTION:隐患|SN:序号|UNIT_NAME:小区名称|CUS_DOM:楼号|CUS_DY:单元|CUS_FLOOR:楼层|CUS_ROOM:房号|USER_NAME:客户姓名|TELPHONE:客户电话|DEPARTURE_TIME:安检日期|PRECAUTION_NOTIFIED:是否下发隐患告知书";
            toExcel.ToExcel();
        }

        void toExcel_Completed(object sender, EventArgs e)
        {
            downLoad.Down();
        }*/

       
    }

}
