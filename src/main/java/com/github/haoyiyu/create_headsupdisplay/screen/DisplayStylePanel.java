package com.github.haoyiyu.create_headsupdisplay.screen;

import com.github.haoyiyu.create_headsupdisplay.api.*;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Mode creator panel — mode list, params, data binding zones, commit button */
public class DisplayStylePanel {
    private static final int PANEL_W = 220, LIST_W = 60, PREVIEW_X = LIST_W + 2, PREVIEW_W = PANEL_W - PREVIEW_X;
    private static final int PREVIEW_H = 55, PARAM_H = 13, ROW_H = 16, BIND_H = 18, MAX_H = 300;

    private int panelX, panelY, draggingTitle, dragOffX, dragOffY;
    private boolean visible;
    private int leftScroll, rightScroll;
    private List<IDisplayMode> allModes = List.of();
    private int hoveredModeIdx = -1;
    private ModeTarget target;
    private final Consumer<String> onParamChange;
    private final Runnable onCommit;
    private int editingField = -1;
    private final List<String> editValues = new ArrayList<>();
    private boolean dataIsNumeric;
    private Font font;
    private long rejectUntil;
    private int rejectIdx;
    // Stored positions from last render
    private final int[] zoneYs = new int[16];
    private int zoneCount;
    private int btnX, btnY, btnW;

    public interface ModeTarget {
        ResourceLocation getDisplayModeId();
        void setDisplayModeId(ResourceLocation id);
        List<String> getDataValues();
        DisplayModeConfig getModeConfig();
        int getColor(); int getAlpha();
        List<BlockPos> getSourcePositions();
        List<String> getSourceNames();
        void bindSource(int index, BlockPos pos, String name, String value);
        void unbindSource(int index);
    }

    public DisplayStylePanel(Consumer<String> onParamChange, Runnable onCommit) {
        this.onParamChange = onParamChange; this.onCommit = onCommit;
    }

    public void show(int x, int y, ModeTarget target) {
        panelX=x; panelY=y; visible=true; leftScroll=0; rightScroll=0; this.target=target;
        allModes=new ArrayList<>(DisplayModeRegistry.getAll());
        String first=target.getDataValues().isEmpty()?"":target.getDataValues().get(0);
        dataIsNumeric=first.isEmpty()||checkNumeric(first);
        var id=target.getDisplayModeId(); hoveredModeIdx=-1;
        if(id!=null)for(int i=0;i<allModes.size();i++)if(allModes.get(i).getId().equals(id)){hoveredModeIdx=i;break;}
        var m=getMode(); editValues.clear();
        if(m!=null)for(var p:m.getConfigParameters())editValues.add(fmt(target.getModeConfig(),p));
        editingField=-1;
    }
    public void hide(){visible=false;editingField=-1;}
    public boolean isVisible(){return visible;}
    private IDisplayMode getMode(){return hoveredModeIdx>=0&&hoveredModeIdx<allModes.size()?allModes.get(hoveredModeIdx):null;}

    private String fmt(DisplayModeConfig c,ConfigParamDescriptor p){return switch(p.type()){
        case FLOAT->{float v=c.getFloat(p.key(),((Number)p.defaultValue()).floatValue());yield v==(int)v?String.valueOf((int)v):String.format("%.1f",v);}
        case INT->String.valueOf(c.getInt(p.key(),((Number)p.defaultValue()).intValue()));
        case STRING->c.getString(p.key(),(String)p.defaultValue());
        case BOOLEAN->String.valueOf(c.getBoolean(p.key(),(Boolean)p.defaultValue()));
        case COLOR->String.format("#%06X",c.getInt(p.key(),((Number)p.defaultValue()).intValue()));
    };}

    public void render(GuiGraphics g, Font font, int mx, int my) {
        this.font=font; if(!visible||target==null)return;
        var cur=getMode(); int totalH=allModes.size()*ROW_H, maxListH=MAX_H-16, listH=Math.min(totalH,maxListH);
        leftScroll=Mth.clamp(leftScroll,0,Math.max(0,totalH-maxListH));
        int pc=cur!=null?cur.getConfigParameters().size():3, act=target.getSourcePositions().size();
        int needed=cur!=null?cur.getMinDataSourceCount():1, dst=Math.max(needed,act);
        int bindH=dst>0?(14+dst*(BIND_H+2)+BIND_H):0, rightH=PREVIEW_H+4+pc*PARAM_H+bindH+8;
        rightScroll=Mth.clamp(rightScroll,0,Math.max(0,rightH-(MAX_H-16)));
        int ph=16+Math.min(Math.max(listH,rightH),MAX_H)+2;
        g.fill(panelX,panelY,panelX+PANEL_W,panelY+ph,0xFF1A1A2E); g.fill(panelX,panelY,panelX+PANEL_W,panelY+1,0xFF6060CC);
        // Title
        int ty=panelY+2; g.fill(panelX+2,ty,panelX+PANEL_W-2,ty+12,0xFF2A2A4A);
        g.drawString(font,Component.translatable("gui.create_headsupdisplay.pro.display_style").getString(),panelX+6,ty+2,0xFFCCCCFF);
        int cx=panelX+PANEL_W-16; boolean ch=mx>=cx&&mx<=cx+12&&my>=ty&&my<=ty+12; g.fill(cx,ty,cx+12,ty+12,ch?0xFFFF4444:0xCC883333); g.drawString(font,"X",cx+3,ty+2,0xFFFFFFFF);
        // Mode list
        int tY=panelY+16; g.enableScissor(panelX,tY,panelX+LIST_W,tY+maxListH);
        int ry=tY-leftScroll;
        for(int i=0;i<allModes.size();i++){var m=allModes.get(i);if(ry+ROW_H>tY&&ry<tY+maxListH){boolean h=mx>=panelX+2&&mx<=panelX+LIST_W&&my>=ry&&my<=ry+ROW_H,isCur=cur!=null&&cur.getId().equals(m.getId()),dis=m.needsNumericData()&&!dataIsNumeric;int bg=isCur?0xFF445577:(h&&!dis?0xFF333355:0xFF222233);g.fill(panelX+2,ry,panelX+LIST_W,ry+ROW_H,bg);if(isCur)g.fill(panelX+2,ry,panelX+5,ry+ROW_H,0xFF4488FF);int nc=dis?0xFF444444:(isCur?0xFF88CCFF:0xFFCCCCCC);g.drawString(font,m.getDisplayName().getString(),panelX+9,ry+(ROW_H-8)/2,nc);if(dis)g.drawString(font,"!",panelX+LIST_W-10,ry+(ROW_H-8)/2,0xFF663333);}ry+=ROW_H;}
        g.disableScissor();
        // Right content
        int px=panelX+PREVIEW_X+4,py=panelY+16,rcy=rightScroll; g.enableScissor(panelX+PREVIEW_X,tY,panelX+PANEL_W,tY+maxListH);
        int pvy=py-rcy; if(pvy+PREVIEW_H>tY&&pvy<tY+maxListH){g.fill(px-2,pvy-2,px+PREVIEW_W-8+2,pvy+PREVIEW_H+2,0xFF111122);g.fill(px-2,pvy-2,px+PREVIEW_W-8+2,pvy,0xFF335577);renderPreview(g,font,px,pvy,PREVIEW_W-8,PREVIEW_H);}
        int rowY=py+PREVIEW_H+2-rcy;
        if(cur!=null){var ps=cur.getConfigParameters();
            for(int fi=0;fi<ps.size();fi++){if(rowY+PARAM_H>tY&&rowY<tY+maxListH)drawParamRow(g,font,mx,my,px,rowY,fi,ps.get(fi).key(),fi<editValues.size()?editValues.get(fi):"");rowY+=PARAM_H;}
            rowY+=4; if(dst>0){int bY=rowY; g.drawString(font,"◎ "+Component.translatable("gui.create_headsupdisplay.pro.bind_sources").getString(),px,bY,0xFFCCCC88);int bzY=bY+11;var pos=target.getSourcePositions();var nms=target.getSourceNames();int bw=PREVIEW_W-8;zoneCount=dst;
                for(int bi=0;bi<dst;bi++){zoneYs[bi]=bzY;if(bzY+BIND_H>tY&&bzY<tY+maxListH){boolean isBound=bi<pos.size()&&pos.get(bi)!=null&&!BlockPos.ZERO.equals(pos.get(bi));String sn=isBound&&bi<nms.size()?nms.get(bi):"";String lb=slotLabel(cur,bi,bi+1);boolean fl=System.currentTimeMillis()<rejectUntil&&rejectIdx==bi;int zc=fl?(System.currentTimeMillis()/100%2==0?0xFF442222:0xFF222233):(isBound?0xFF1A3322:0xFF222233),bc=fl?0xFFFF4444:(isBound?0xFF44AA44:0x66555577);g.fill(px,bzY,px+bw,bzY+BIND_H,zc);
                    if(isBound){g.fill(px,bzY,px+bw,bzY+1,bc);g.fill(px,bzY+BIND_H-1,px+bw,bzY+BIND_H,bc);g.fill(px,bzY,px+1,bzY+BIND_H,bc);g.fill(px+bw-1,bzY,px+bw,bzY+BIND_H,bc);String d=sn.length()>14?sn.substring(0,13)+".":sn;g.drawString(font,lb+": "+d,px+4,bzY+2,0xFF44CC44);int ux=px+bw-14,bt=bzY+2;boolean uh=mx>=ux&&mx<=ux+10&&my>=bt&&my<=bt+10;g.fill(ux,bt,ux+10,bt+10,uh?0xFFFF4444:0xCC883333);g.drawString(font,"✕",ux+2,bt+1,0xFFFFFFFF);}else{dash(g,px,bzY,px+bw,bzY+BIND_H,0x66555577);g.drawString(font,lb+": "+Component.translatable("gui.create_headsupdisplay.pro.bind_drop_hint").getString(),px+4,bzY+2,fl?0xFFFF8888:0xFF666688);}}bzY+=BIND_H+2;}
                bzY+=1; String al=Component.translatable("gui.create_headsupdisplay.pro.commit_to_canvas").getString(); btnW=font.width("  "+al+"  ")+2; btnX=px+bw-btnW; btnY=bzY; boolean ah=mx>=btnX&&mx<=btnX+btnW&&my>=bzY&&my<=bzY+BIND_H; g.fill(btnX,bzY,btnX+btnW,bzY+BIND_H,ah?0xFF446655:0xFF2A3A2A); g.drawString(font,al,btnX+4,bzY+2,ah?0xFF88FFAA:0xFF668866);}}
        g.disableScissor();
        if(!dataIsNumeric&&cur!=null&&cur.needsNumericData())g.drawString(font,Component.translatable("gui.create_headsupdisplay.pro.data_not_numeric").getString(),px,py+PREVIEW_H/2-4-rcy,0xFFFF4444);
        if(editingField>=0)g.drawString(font,"Enter→save  Esc→cancel",px,tY+maxListH+2,0xFF8888AA);
    }
    private String slotLabel(IDisplayMode m,int i,int n){var ps=m.getConfigParameters();if(ps.size()>=3&&m.getMinDataSourceCount()>=3){for(var p:ps){if(p.key().equals("labelX")&&i==0)return target.getModeConfig().getString("labelX","X");if(p.key().equals("labelY")&&i==1)return target.getModeConfig().getString("labelY","Y");if(p.key().equals("labelZ")&&i==2)return target.getModeConfig().getString("labelZ","Z");}}return Component.translatable("gui.create_headsupdisplay.pro.bind_slot").getString()+n;}
    private void renderPreview(GuiGraphics g,Font font,int x,int y,int w,int h){if(target==null)return;var m=getMode();if(m==null)return;var dv=target.getDataValues();if(dv==null||dv.isEmpty())dv=List.of("");g.pose().pushPose();g.pose().translate(x,y,0);m.renderPreview(g,font,dv,target.getModeConfig(),w,h);g.pose().popPose();}
    private void drawParamRow(GuiGraphics g,Font font,int mx,int my,int px,int y,int fi,String key,String val){String lb=Component.translatable("gui.create_headsupdisplay.pro.param."+key).getString();if(lb.equals("gui.create_headsupdisplay.pro.param."+key))lb=key;g.drawString(font,lb+":",px,y+1,0xFF8888CC);int vx=px+font.width(lb+":")+4,vw=60;boolean h=mx>=vx&&mx<=vx+vw&&my>=y&&my<=y+PARAM_H,ed=editingField==fi;g.fill(vx,y,vx+vw,y+PARAM_H,ed?0xFF334466:(h?0xFF222244:0xFF1A1A30));g.drawString(font,ed?val+"_":val,vx+2,y+1,ed?0xFFFFFFFF:0xFFAAAAAA);}

    private void dash(GuiGraphics g,int x1,int y1,int x2,int y2,int c){int d=3;for(int x=x1;x<x2;x+=d*2)g.fill(x,y1,Math.min(x+d,x2),y1+1,c);for(int x=x1;x<x2;x+=d*2)g.fill(x,y2-1,Math.min(x+d,x2),y2,c);for(int y=y1;y<y2;y+=d*2)g.fill(x1,y,x1+1,Math.min(y+d,y2),c);for(int y=y1;y<y2;y+=d*2)g.fill(x2-1,y,x2,Math.min(y+d,y2),c);}

    public boolean mouseClicked(double mx,double my,int btn){if(!visible)return false;if(unbindClicked(mx,my))return true;if(commitClicked(mx,my))return true;int ph=16+MAX_H+2,ty=panelY+2,cx=panelX+PANEL_W-16;if(mx>=cx&&mx<=cx+12&&my>=ty&&my<=ty+12){visible=false;return true;}if(mx>=panelX+2&&mx<=panelX+PANEL_W-18&&my>=ty&&my<=ty+12){draggingTitle=1;dragOffX=(int)(mx-panelX);dragOffY=(int)(my-panelY);return true;}int tY=panelY+16,ry=tY-leftScroll;for(int i=0;i<allModes.size();i++){if(mx>=panelX+2&&mx<=panelX+LIST_W&&my>=ry&&my<=ry+ROW_H){var m=allModes.get(i);if(!(m.needsNumericData()&&!dataIsNumeric)&&target!=null){target.setDisplayModeId(m.getId());hoveredModeIdx=i;if(onParamChange!=null)onParamChange.accept("mode");var cfg=target.getModeConfig();editValues.clear();for(var p:m.getConfigParameters())editValues.add(fmt(cfg,p));}return true;}ry+=ROW_H;}var cur=getMode();if(cur!=null){int py=panelY+16+PREVIEW_H+2-rightScroll;for(int fi=0;fi<cur.getConfigParameters().size();fi++){int vx=panelX+PREVIEW_X+4+font.width(Component.translatable("gui.create_headsupdisplay.pro.param."+cur.getConfigParameters().get(fi).key()).getString()+":")+8;if(mx>=vx&&mx<=vx+60&&my>=py&&my<=py+PARAM_H){editingField=fi;return true;}py+=PARAM_H;}}if(my<panelY||my>panelY+ph||mx<panelX||mx>panelX+PANEL_W)return false;return false;}
    public boolean mouseDragged(double mx,double my){if(!visible||draggingTitle==0)return false;panelX=(int)mx-dragOffX;panelY=(int)my-dragOffY;return true;}
    public boolean mouseReleased(){draggingTitle=0;return false;}
    public boolean mouseScrolled(double mx,double my,double sy){if(!visible)return false;int tY=panelY+16;if(mx>=panelX&&mx<=panelX+LIST_W&&my>=tY&&my<=tY+MAX_H-16){leftScroll-= (int)sy*16;leftScroll=Mth.clamp(leftScroll,0,Math.max(0,allModes.size()*ROW_H-(MAX_H-16)));return true;}if(mx>=panelX+PREVIEW_X&&mx<=panelX+PANEL_W&&my>=tY&&my<=tY+MAX_H-16){rightScroll-= (int)sy*16;return true;}return false;}
    public boolean keyPressed(int kc){if(!visible||editingField<0||editingField>=editValues.size())return false;String c=editValues.get(editingField);if(kc==259&&!c.isEmpty())editValues.set(editingField,c.substring(0,c.length()-1));else if(kc==257){applyEdit();editingField=-1;}else if(kc==256)editingField=-1;return true;}
    public boolean charTyped(char ch){if(!visible||editingField<0)return false;var m=getMode();if(m==null||editingField>=m.getConfigParameters().size())return false;var t=m.getConfigParameters().get(editingField).type();String c=editValues.get(editingField);boolean v=switch(t){case FLOAT,INT->(ch>='0'&&ch<='9')||ch=='.'||ch=='-';case BOOLEAN->ch=='t'||ch=='f'||ch=='T'||ch=='F';case COLOR->(ch>='0'&&ch<='9')||(ch>='a'&&ch<='f')||(ch>='A'&&ch<='F')||ch=='#';case STRING->ch>=32;};if(v&&c.length()<16)editValues.set(editingField,c+ch);return true;}
    private void applyEdit(){var m=getMode();if(m==null||target==null)return;var cfg=target.getModeConfig();var ps=m.getConfigParameters();for(int i=0;i<ps.size()&&i<editValues.size();i++){var p=ps.get(i);String r=editValues.get(i);try{switch(p.type()){case FLOAT->cfg.setFloat(p.key(),Float.parseFloat(r));case INT->cfg.setInt(p.key(),Integer.parseInt(r));case STRING->cfg.setString(p.key(),r);case BOOLEAN->cfg.setBoolean(p.key(),Boolean.parseBoolean(r));case COLOR->cfg.setInt(p.key(),(int)Long.parseLong(r.replace("#",""),16));}}catch(NumberFormatException ignored){}if(onParamChange!=null)onParamChange.accept(p.key()+":"+r);}}

    public boolean unbindClicked(double mx,double my){if(!visible||target==null)return false;int px=panelX+PREVIEW_X+4;for(int bi=0;bi<zoneCount;bi++){int ux=px+PREVIEW_W-8-14;if(mx>=ux&&mx<=ux+10&&my>=zoneYs[bi]+2&&my<=zoneYs[bi]+12){target.unbindSource(bi);return true;}}return false;}
    private boolean commitClicked(double mx,double my){if(target==null||onCommit==null)return false;if(mx>=btnX&&mx<=btnX+btnW&&my>=btnY&&my<=btnY+BIND_H){onCommit.run();return true;}return false;}
    public int bindZoneAt(double mx,double my){if(!visible||target==null)return -1;int px=panelX+PREVIEW_X+4;for(int bi=0;bi<zoneCount;bi++)if(mx>=px&&mx<=px+PREVIEW_W-8&&my>=zoneYs[bi]&&my<=zoneYs[bi]+BIND_H)return bi;return -1;}
    public void rejectBind(int zi){rejectUntil=System.currentTimeMillis()+500;rejectIdx=zi;}
    public static boolean isNumeric(String t){return checkNumeric(t);}
    private static boolean checkNumeric(String t){String s=t.replaceAll("§[0-9a-fk-or]","").trim();int sp=s.indexOf(' ');if(sp>0)s=s.substring(0,sp);try{Float.parseFloat(s);return true;}catch(NumberFormatException e){return false;}}
}
