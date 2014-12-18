using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Runtime.InteropServices;
using Com.Aote.Logs;

namespace Card
{
    public class JinKaMHK : ICard
    {
        private static Log Log = Log.GetInstance("Card.JinKaMHK");

        public string Test()
        {
            return "ruisen";
        }

        #region 金卡动态库导入
        //读卡，标准接口
        [DllImport("goldcard.dll", EntryPoint = "ReadGasCard", CallingConvention = CallingConvention.StdCall)]
        public static extern int StaticReadGasCard(Int16 com, Int32 baut,byte[] kmm, ref Int16 klx, ref Int16 kzt,  
            byte[] kh, byte[] dqdm, byte[] yhh, byte[] tm, ref Int32 ql, ref Int16 cs, ref Int32 ljgql, ref Int16 bkcs, 
            ref Int32 ljyql, ref Int32 syql, ref Int32 bjql, ref Int32 czsx, ref Int32 tzed,byte[] sqrq ,ref Int32 oldprice, 
            ref Int32 newprice, byte[] sxrq, byte[] sxbj);
        //写新卡，标准接口
        [DllImport("goldcard.dll", EntryPoint = "WriteNewCard", CallingConvention = CallingConvention.StdCall)]
        public static extern int StaticWriteNewCard(Int16 com, Int32 baut, byte[] kmm, Int16 klx, Int16 kzt,
            byte[] kh, byte[] dqdm, byte[] yhh, byte[] tm, Int32 ql, Int16 cs, Int32 ljgql, Int16 bkcs, Int32 ljyql,
            Int32 bjql, Int32 czsx, Int32 tzed, byte[] sqrq, ref Int32 oldprice, ref Int32 newprice, byte[] sxrq, byte[] sxbj);
        //测卡，标准接口
        [DllImport("goldcard.dll", EntryPoint = "CheckGasCard", CallingConvention = CallingConvention.StdCall)]
        public static extern int StaticCheckGasCard(Int16 com, Int32 baut);
        //格式化卡，标准接口
        [DllImport("goldcard.dll", EntryPoint = "FormatGasCard", CallingConvention = CallingConvention.StdCall)]
        public static extern int StaticFormatGasCard(Int16 com, Int32 baut, byte[] kmm, Int16 klx, byte[] kh, byte[] dqdm);
        //写购气卡，标准接口
        [DllImport("goldcard.dll", EntryPoint = "WriteGasCard", CallingConvention = CallingConvention.StdCall)]
        public static extern int StaticWriteGasCard(Int16 com, Int32 baut, byte[] kmm, Int16 klx, byte[] kh, 
            byte[] dqdm, Int32 ql, Int16 cs, Int32 ljgql, Int32 bjql, Int32 czsx, Int32 tzed,
            byte[] sqrq ,ref Int32 oldprice, ref Int32 newprice, byte[] sxrq, byte[] sxbj);
        #endregion

        #region ICard Members

        public int CheckGasCard(short com, int baud)
        {
            int ret = -1;
            try
            {
                Int16 com1 = (Int16)com;
                Int32 baud1 = (Int32)baud;
                Log.Debug("JinKa CheckGasCard start");
                ret = StaticCheckGasCard(com1, baud1);
                Log.Debug("JinKa CheckGasCard end,return:"+ret);
                if(0 == ret)
                {
                    byte[] cardNO = new byte[100];
                    byte[] kmm = new byte[100];
                    byte[] dqdm = new byte[100];
                    byte[] yhh = new byte[100];
                    byte[] sqrq = new byte[100];
                    byte[] sxrq = new byte[100];
                    byte[] sxbj = new byte[100];
                    byte[] tm = new byte[100];
                    short klx = 0;
                    short kzt = 0;
                    int ljgql = 0;
                    int ljyql = 0;
                    int ql = 0;
                    short cs = 0;
                    short bkcs = 0;
                    int syql = 0;
                    int bjql = 0;
                    int czsx = 0;
                    int tzed = 0;
                    int oldprice = 0;
                    int newprice = 0;
                    ret = StaticReadGasCard(0, baud, kmm, ref klx, ref kzt, cardNO, dqdm, yhh, tm,
                   ref ql, ref cs, ref ljgql, ref bkcs, ref ljyql, ref syql, ref bjql, ref czsx,
                   ref tzed, sqrq, ref oldprice, ref newprice, sxrq, sxbj);
                    Log.Debug("klx:"+klx);
                    if (klx == 1)
                    {
                        Log.Debug("此卡是金卡民用卡！");
                        return 0;
                    }
                    Log.Debug("此卡不是金卡民用卡！");
                    return -1;
                }
                Log.Debug("此卡不是金卡民用卡！");
                return -1;
            }
            catch(Exception e)
            {
                Log.Debug("金卡民用判卡异常：" + e.Message + "--" + e.StackTrace);
            }
            return ret;
        }

        public int FormatGasCard(short com, int baud, string kmm, string kh, string dqdm)
        {
            int ret = -1;
            try
            {
                Int16 com1 = (Int16)com;
                Int32 baud1 = (Int32)baud;
                Int16 klx = 1;
                byte[] mm = System.Text.Encoding.GetEncoding(1252).GetBytes(kmm);
                byte[] cardNO = System.Text.Encoding.GetEncoding(1252).GetBytes(kh);
                byte[] bdqdm = System.Text.Encoding.GetEncoding(1252).GetBytes(dqdm);
                Log.Debug("JinKa FormatGasCard start");
                Log.Debug("kmm --" + mm + "kh --" + cardNO);
                ret = StaticFormatGasCard(com, baud, mm, klx, cardNO, bdqdm);
                Log.Debug("JinKa FormatGasCard end,return:"+ret);
                if (0 == ret)
                {
                    Log.Debug("金卡民用擦卡成功！");
                    return 0;
                }
                else
                {
                    Log.Debug("金卡民用擦卡失败！");
                    return -1;
                }
            }
            catch (Exception e)
            {
                Log.Debug("金卡民用擦卡异常：" + e.Message + "--" + e.StackTrace);
            }
            return ret;
        }


        public int WriteGasCard(short com, int baud, ref string kmm, string kh, string dqdm, int ql, int csql, int ccsql, short cs, int ljgql, int bjql, int czsx, int tzed, string sqrq, string cssqrq, int oldprice, int newprice, string sxrq, string sxbj)
        {
            int ret = -1;
            try
            {
                Int16 com1 = (Int16)com;
                Int32 baud1 = (Int32)baud;  
                Int16 klx = 1; 
                Int32 ql1 = (Int32)ql;
                Int16 cs1 = (Int16)cs;
                Int32 ljgql1 = (Int32)ljgql;
                Int32 bjql1 = (Int32)bjql;
                Int32 czsx1 = (Int32)czsx;
                Int32 tzed1 = (Int32)tzed;
                Int32 oldprice1 = (Int32)oldprice;
                Int32 newprice1 = (Int32)newprice;
                byte[] mm = System.Text.Encoding.GetEncoding(1252).GetBytes(kmm);
                byte[] cardNO = System.Text.Encoding.GetEncoding(1252).GetBytes(kh);
                byte[] bdqdm = System.Text.Encoding.GetEncoding(1252).GetBytes(dqdm);
                byte[] bsqrq = System.Text.Encoding.GetEncoding(1252).GetBytes(sqrq);
                byte[] bsxrq = System.Text.Encoding.GetEncoding(1252).GetBytes(sxrq);
                byte[] bsxbj = System.Text.Encoding.GetEncoding(1252).GetBytes(sxbj);
                Log.Debug("JinKa WriteGasCard start");
                Log.Debug("kh：" + cardNO+"klx："+klx+"ql："+ql+"cs："+cs);
                ret = StaticWriteGasCard(com1, baud1, mm, klx, cardNO, bdqdm, ql1,
                    cs1, ljgql1, bjql1, czsx1, tzed1, bsqrq, ref oldprice1, ref newprice1, bsxrq, bsxbj);
                Log.Debug("JinKa WriteGasCard end,return:"+ret);
                if (0 == ret)
                {
                    Log.Debug("金卡民用购气成功！");
                    return 0;
                }
                else 
                {
                    Log.Debug("金卡民用购气失败！");
                    return -1;
                }
            }
            catch (Exception e)
            {
                Log.Debug("金卡民用购气卡异常：" + e.Message + "--" + e.StackTrace);
            }
            return ret;
        }


        public string Name
        {
            get
            {
                return "jinkamhk";
            }
        }

        public int ReadGasCard(short com, int baud, ref string kh, ref int ql, ref decimal money, ref short cs, ref short bkcs, ref string dqdm)
        {
            int ret = -1;
            try
            {
                Int16 com1 = (Int16)com; 
                Int32 baut1 = (Int32)baud;
                Int16 klx = 0;
                Int16 kzt = 0;
                Int32 ql1 = (Int32)ql;
                Int16 cs1 = (Int16)cs;
                Int32 ljgql = 0;
                Int16 bkcs1 = (Int16)bkcs; 
                Int32 ljyql = 0;
                Int32 syql = 0;
                Int32 bjql = 0; 
                Int32 czsx = 0;
                Int32 tzed = 0;
                Int32 oldprice = 0; 
                Int32 newprice = 0;
                byte[] cardNO = new byte[100];
                byte[] kmm = new byte[100];
                byte[] bdqdm = new byte[100];
                byte[] yhh = new byte[100];
                byte[] sqrq = new byte[100];
                byte[] sxrq = new byte[100];
                byte[] sxbj = new byte[100];
                byte[] tm = new byte[100];
                Log.Debug("JinKa ReadGasCard start");
                ret = StaticReadGasCard(0, baud, kmm, ref klx, ref kzt, cardNO, bdqdm, yhh, tm,
                    ref ql1, ref cs1, ref ljgql, ref bkcs1, ref ljyql, ref syql, ref bjql, ref czsx,
                    ref tzed, sqrq, ref oldprice, ref newprice, sxrq, sxbj);
                //卡号转换成字符串
                cardNO[8] = 0;
                kh = Encoding.ASCII.GetString(cardNO, 0, 8);
                Log.Debug("JinKa ReadGasCard end,return:" + ret);
                ql = ql1;
                cs = cs1;
                bkcs = bkcs1;
                if (0 == ret || 1 == klx)
                {
                    Log.Debug("金卡民用读卡成功！");
                    Log.Debug("kh：" + kh + "ql：" + ql + "klx：" + klx + "cs：" + cs);
                    return 0;
                }
                else
                {
                    Log.Debug("金卡民用读卡失败！");
                    return -1;
                }
            }
            catch (Exception e)
            {
                Log.Debug("金卡民用读卡异常：" + e.Message + "--" + e.StackTrace);
            }
            return ret;
        }

        public int WriteNewCard(short com, int baud, ref string kmm, short kzt, string kh, string dqdm, string yhh, string tm, int ql, int csql, int ccsql, short cs, int ljgql, short bkcs, int ljyql, int bjql, int czsx, int tzed, string sqrq, string cssqrq, int oldprice, int newprice, string sxrq, string sxbj)
        {
            int ret = -1;
            try
            {
                Int16 com1 = (Int16)com;
                Int32 baud1 = (Int32)baud;
                Int16 klx = 0;
                Int16 rkzt = 0;
                Int32 rql = 0;
                Int16 rcs = 0;
                Int32 rljgql = 0;
                Int16 rbkcs = 0;
                Int32 rljyql = 0;
                Int32 rsyql = 0;
                Int32 rbjql = 0;
                Int32 rczsx = 0;
                Int32 rtzed = 0;
                Int32 roldprice = 0;
                Int32 rnewprice = 0;
                byte[] rcardNO = new byte[100];
                byte[] rkmm = new byte[100];
                byte[] rdqdm = new byte[100];
                byte[] ryhh = new byte[100];
                byte[] rsqrq = new byte[100];
                byte[] rsxrq = new byte[100];
                byte[] rsxbj = new byte[100];
                byte[] rtm = new byte[100];
                Log.Debug("JinKa ReadGasCard start");
                ret = StaticReadGasCard(com1, baud1, rkmm, ref klx, ref rkzt, rcardNO, rdqdm, ryhh, rtm,
                    ref rql, ref rcs, ref rljgql, ref rbkcs, ref rljyql, ref rsyql, ref rbjql, ref rczsx,
                    ref rtzed, rsqrq, ref roldprice, ref rnewprice, rsxrq, rsxbj);
                Log.Debug("JinKa ReadGasCard end,return:" + ret);
                rkmm[8] = 0;
                rcardNO[8] = 0;
                rdqdm[8] = 0;
                string kmm2 = Encoding.ASCII.GetString(rkmm, 0, 8);
                string kh2 = Encoding.ASCII.GetString(rcardNO, 0, 8);
                string dqdm2 = Encoding.ASCII.GetString(rdqdm, 0, 8);
                byte[] ckmm = System.Text.Encoding.GetEncoding(1252).GetBytes(kmm2);
                byte[] ckh = System.Text.Encoding.GetEncoding(1252).GetBytes(kh2);
                byte[] cdqdm = System.Text.Encoding.GetEncoding(1252).GetBytes(dqdm2);
                Int16 kzt1 = (Int16)kzt;
                Int32 ql1 = (Int32)ql;
                Int16 cs1 = (Int16)cs;
                Int32 ljgql1 = (Int32)ljgql;
                Int16 bkcs1 = (Int16)bkcs;
                Int32 ljyql1 = (Int32)ljyql;
                Int32 bjql1 = (Int32)bjql;
                Int32 czsx1 = (Int32)czsx;
                Int32 tzed1 = (Int32)tzed;
                byte[] mm = new byte[10];
                byte[] cardNO = System.Text.Encoding.GetEncoding(1252).GetBytes(kh);
                byte[] bdqdm = System.Text.Encoding.GetEncoding(1252).GetBytes(dqdm);
                byte[] byhh = System.Text.Encoding.GetEncoding(1252).GetBytes(yhh);
                byte[] btm = new byte[10];
                byte[] bsqrq = new byte[10];
                byte[] bsxrq = new byte[10];
                byte[] bsxbj = new byte[10];
                Int32 oldprice1 = (Int32)oldprice;
                Int32 newprice1 = (Int32)newprice;
                if (0 == ret)
                {
                    Log.Debug("JinKa FormatGasCard start");
                    //发卡前先格式化卡
                    ret = StaticFormatGasCard(com1, baud1, ckmm, klx, ckh, cdqdm);
                    Log.Debug("JinKa FormatGasCard end,return:" + ret);
                    Log.Debug("JinKa WriteNewCard start");
                    Log.Debug("kh：" + cardNO + "klx:" + 1 + "ql:" + ql + "cs" + cs);
                    ret = StaticWriteNewCard(com1, baud1, mm, 1, kzt1, cardNO, bdqdm, byhh, btm,
                        ql1, cs1, ljgql1, bkcs1, ljyql1, bjql1, czsx1, tzed1, bsqrq, ref oldprice1, ref newprice1, bsxrq, bsxbj);
                    Log.Debug("JinKa WriteNewCard end,return:" + ret);
                }
                else
                {
                    Log.Debug("JinKa WriteNewCard start");
                    Log.Debug("kh：" + cardNO + "klx:" + 1 + "ql:" + ql + "cs" + cs);
                    ret = StaticWriteNewCard(com1, baud1, mm, 1, kzt1, cardNO, bdqdm, byhh, btm,
                        ql1, cs1, ljgql1, bkcs1, ljyql1, bjql1, czsx1, tzed1, bsqrq, ref oldprice1, ref newprice1, bsxrq, bsxbj);
                    Log.Debug("JinKa WriteNewCard end,return:" + ret);
                }
                if (0 == ret)
                {
                    Log.Debug("金卡民用写新卡成功！");
                    return 0;
                }
                else
                {
                    Log.Debug("金卡民用写新卡失败！");
                    return -1;
                }
            }
            catch (Exception e)
            {
                Log.Debug("金卡民用写新卡异常：" + e.Message + "--" + e.StackTrace);
            }
            return ret;
        }
        #endregion
    }
}
