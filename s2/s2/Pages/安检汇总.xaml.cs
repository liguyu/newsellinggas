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
    public partial class 安检汇总 : UserControl
    {
        public 安检汇总()
        {
            InitializeComponent();
        }

        private void btnSearch_Click(object sender, RoutedEventArgs e)
        {
            String where = "1=1";
            String dt = "1=1";
            if (StartDate.Text.Trim().Length != 0)
                dt = " DEPARTURE_TIME>='" + StartDate.Text + "'";
            if (EndDate.Text.Trim().Length != 0)
            {
                if (dt.Length > 3)
                    dt += " and DEPARTURE_TIME<='" + EndDate.Text + " 23:59:59'";
                else
                    dt = " DEPARTURE_TIME<='" + EndDate.Text + " 23:59:59'";
            }
            String dt2 = "1=1";
            if (StartDate.Text.Trim().Length != 0)
                dt2 = " SAVE_DATE>='" + StartDate.Text + "'";
            if (EndDate.Text.Trim().Length != 0)
            {
                if (dt2.Length > 3)
                    dt2 += " and SAVE_DATE<='" + EndDate.Text + " 23:59:59'";
                else
                    dt2 = " SAVE_DATE<='" + EndDate.Text + " 23:59:59'";
            }
            checkerList.Path = "sql";
            checkerList.Names = "jiancha,ruhu,wuren,jujian,louqi,biao,tongzhishu";
            String sql = @"select jiancha, ruhu, wuren, jujian, (select count(id) from T_INSPECTION_LINE where inspection_id is not null and content like '%漏气%' and {0}) louqi,  
(select count(DISTINCT inspection_id) from T_INSPECTION_LINE where inspection_id is not null and equipment = '燃气表' and content in('长通表', '死表', '表不过气', '其他') and {0}) biao,  
(select count(id) from T_INSPECTION_LINE where inspection_id is not null and content = '已发近期安检报告书' and {0}) tongzhishu
FROM(
select count(id) jiancha, sum(ruhu) ruhu, sum(wuren) wuren, sum(jujian) jujian  from (
select id, cast(case condition when '正常' then 1 else 0 end as INTEGER) ruhu, cast(case condition when '无人' then 1 else 0 end as INTEGER) wuren, cast(case condition when '拒绝' then 1 else 0 end as INTEGER) jujian from T_INSPECTION where (deleted is null or deleted!='是')  and  {1}  
) t ) t";
            checkerList.HQL = String.Format(sql, new String[] { dt2, dt });
            checkerList.Load();
        }
    }
}

