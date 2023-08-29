import java.util.ArrayList;
import java.util.HashSet;
import java.util.TreeSet;
import java.util.Arrays;

public class MsJ1200036 extends MsPlayer {
    private boolean start;                   //先に始めるならtrue、二番手ならfalse
    private int turn;                        //初回のオープン前は0、初回のオープンをしたら1になる
    private int height, width;               //盤面の高さと幅
    private boolean result;                  //resultはオープンの戻り値を受ける
    private String chosenCell;               //オープンかフラグを立てるセル
    private TreeSet<String> bombSet = new TreeSet<>();                   //爆弾であると判断されたらこのセットに入れる
    private TreeSet<String> emptySet = new TreeSet<>();                  //爆弾ではないと判断されたらこのセットに入れる
    private TreeSet<String> opdSet = new TreeSet<>();                    //周囲に-1のセルがあって、自身がオープンされたセルの集合
    private ArrayList<String> unopdL = new ArrayList<>();                //全ての-1のセルの集合
    private ArrayList<HashSet<String>> constraint = new ArrayList<>();   //[{0102, 0103}, {0304, 0405}...]のようにセルをグループ分けして格納する
    private ArrayList<Integer> cons_bomb = new ArrayList<>();            //[1, 2, 3]のように、constraintの排列の中の各Setそれぞれの爆弾数を格納する。
                                                                         //例：0102と0103に爆弾が１個あるという意味。

    //Constructor
    public MsJ1200036(){ super(); }
	public MsJ1200036(MsBoard board){ 
        super(board); 
        turn = 0;
    }


    public boolean play(){
        height = board.getHeight();
        width = board.getWidth();
        
        //一番手で初回のオープンの場合は中央のマスをオープンする
        if(this.id == 0) start = true;
        else start = false;
        if(start == true && turn == 0){
            result = board.openCell(width/2, height/2);    
            turn = 1;
            return result;
        }
        
        //爆弾集合（bombSet）と空きマス（emptySet）集合を更新
        updateBombSet();
        updateEmptySet();

        //シンプル戦略とアドバンスト戦略を実行し、opdSetを元に戻す
        SimpleStrategy();
        AdvancedStrategy();
        opdSet.clear();

        /*
        爆弾集合が空いてない場合、その中で一番多くの点数が取れるマスを選んでフラグを立てる。（seekMaxScoreメソッド）
        爆弾集合が空集合なら、空きマス集合が空集合でなければ、そこから一番目のマスを取ってきてオープンする。
        空きマス集合も空集合の場合、四つの角のマスどちらもオープンされなければ左上のをオープン。
        それでも無理なら、残りの未オープンの全てのマスから、爆弾オープンの場合一番減点の少ないマスを選び出してオープンする（seekMinPenalty）
        */
        if(bombSet.size() != 0){
            chosenCell = seekMaxScore(bombSet);
            result = board.putFlag(parse(chosenCell)[0], parse(chosenCell)[1]);
            bombSet.remove(chosenCell);
            return result;
        }
        else{
            if(emptySet.size() != 0){
                chosenCell = emptySet.pollFirst();
                result = board.openCell(parse(chosenCell)[0], parse(chosenCell)[1]);
                return result;
            }
            else{
                if(board.getCell(0, 0) == -1 && board.getCell(0, height-1) == -1 && 
                board.getCell(width-1, 0) == -1 && board.getCell(width-1, height-1) == -1){
                    result = board.openCell(0, 0);
                    return result;
                }
                else{
                    unopdL = findUnopenedGrid();
                    chosenCell = seekMinPenalty(unopdL);
                    result = board.openCell(parse(chosenCell)[0], parse(chosenCell)[1]);
                    unopdL.clear();
                    return result;
                }
            }
        }
    }


    /*以下は二つの戦略----------------------------------------------------------------------------------------
    シンプル戦略
    あるマスの周囲の未オープンマス数＝残りの爆弾数、未オープンのマスは全部爆弾
    あるマスの周囲の標記された爆弾数＝このマスの数字、周囲の未オープンのマスは全部空き
    */
    private boolean SimpleStrategy(){
        boolean ifOperated = false;    //爆弾を見つけたかどうかの状態を表す
        opdSet = findMeetGrid();       //周囲に-1のセルがあって、かつ自身がオープンされたセルを探す

        for(String i : opdSet){
            int x = parse(i)[0];
            int y = parse(i)[1];
            int[][] indices = getNeighborIndices(x, y);

            //あるマスの周囲の未オープン数＝残りの爆弾数、未オープンのマスは全部爆弾
            if(remainingBomb(x, y) == num_unopenedCell(x, y)){
                for(int[] j : indices){
                    if(board.getCell(j[0], j[1]) == -1){ 
                        bombSet.add(load(j[0], j[1]));
                        ifOperated = true;
                    }    
                }
            }

            //あるマスの周囲の標記した爆弾数＝このマスの数字、未オープンは全部空き
            if(bombNum(x, y) == board.getCell(x, y)){
                for(int[] k : indices){
                    if(board.getCell(k[0], k[1]) == -1) emptySet.add(load(k[0], k[1]));
                }
            }
        }
        opdSet.clear();
        return ifOperated;          //爆弾を見つけたかどうかの状態を返す
    }

    /*
    アドバンスト戦略
    何個のマスに爆弾何個あるのかという制約条件を使って爆弾マスと空きマスを探す。
    */
    private boolean AdvancedStrategy(){
        boolean ifOperated = false;        //爆弾を見つけたかどうかの状態を表す
        opdSet = findMeetGrid();           //周囲に-1のセルがあって、かつ自身がオープンされたセルを探す

        for(String i : opdSet){
            int x = parse(i)[0];
            int y = parse(i)[1];
            HashSet<String> neighborCells = new HashSet<>();

            /*
            ここで制約条件を洗い出す
            セルiの周りの未オープンのマスをneighborCellというSetに入れて、
            このSetをconstraintという配列に入れる
            各neighborCellの爆弾数をcons_bombに入れる。
　　　　　　　constraintのI番目のSetの爆弾数はcons_bombのI番目にある。
            */
            int[][] indices = getNeighborIndices(x, y);
            for(int j = 0; j < indices.length; j++){
                if(board.getCell(indices[j][0], indices[j][1]) == -1){
                    neighborCells.add(load(indices[j][0], indices[j][1]));
                }
            }
            constraint.add(neighborCells);
            cons_bomb.add(remainingBomb(x, y));
        }

        /*
        constraintにSetが2つ以上ある場合しかアドバンスト戦略が使えない。
        Advanced戦略では、constraintにあるSetを二つずつ取り出して、以下の四つのパターンにあたるか否かをチェックする。
　　　　1．二つの集合が完全に同じなら、continue
　　　　2．一つの集合がもう一つの部分集合で、かつ爆弾数が同じなら、差集合をemptySetに入れる
　　　　3．爆弾がより多い集合の長さ-和集合の長さ＝爆弾数の差なら、より多くの爆弾を持っている集合と和集合の差集合を爆弾集合に入れる
　　　　4．二つの集合の爆弾数が同じで、かつ和集合の大きさ＝二つの集合の爆弾数＝盤面の残りの爆弾数なら、和集合を爆弾集合に入れる。
        */
        if(constraint.size() < 2){
            opdSet.clear();
            constraint.clear();
            cons_bomb.clear();
            return false;
        }
        else{
            for(int i = 0; i < constraint.size(); i++){

                for(int j = i + 1; j < constraint.size(); j++){ 
                    HashSet<String> setI = new HashSet<>(constraint.get(i));
                    HashSet<String> setJ = new HashSet<>(constraint.get(j));                 

                    //1．二つの集合が完全に同じなら、continue
                    if(setI.size() == setJ.size() && setI.containsAll(setJ)){
                        continue;
                    }

                    //2．一つの集合がもう一つの部分集合で、かつ爆弾数が同じなら、差集合をemptySetに入れる
                    if((setI.containsAll(setJ) || setJ.containsAll(setI)) == true && cons_bomb.get(i) == cons_bomb.get(j)){
                        HashSet<String> temp = new HashSet<>();
                        if(setI.size() > setJ.size()){
                            temp.addAll(setI);
                            temp.removeAll(setJ);
                        }
                        else{
                            temp.addAll(setJ);
                            temp.removeAll(setI);
                        }
                        //System.out.println(setI.toString() + " + " + setJ.toString() + " = " + temp.toString());
                        emptySet.addAll(temp);
                    }

                    //workbenchに和集合を入れる
                    HashSet<String> workbench = new HashSet<>();
                    workbench.addAll(setI);
                    workbench.retainAll(setJ);

                    //3．爆弾がより多い集合の長さ-和集合の長さ＝爆弾数の差なら、より多くの爆弾を持っている集合と和集合の差集合を爆弾集合に入れる
                    if(cons_bomb.get(i) != cons_bomb.get(j)){
                        HashSet<String> moreBombs = (cons_bomb.get(i) > cons_bomb.get(j)) ? setI : setJ;
                        if((moreBombs.size() - workbench.size()) == Math.abs(cons_bomb.get(i) - cons_bomb.get(j))){
                            moreBombs.removeAll(workbench);
                            bombSet.addAll(moreBombs);
                            ifOperated = true;
                        }
                    }

                    //4．二つの集合の爆弾数が同じで、かつ和集合の大きさ＝二つの集合の爆弾数＝盤面の残りの爆弾数なら、和集合を爆弾集合に入れる。
                    if(workbench.size() >= 1 && workbench.size() == cons_bomb.get(i) && 
                       workbench.size() == cons_bomb.get(j) && workbench.size() == AllRemainingBombs()){
                        bombSet.addAll(workbench);
                        ifOperated = true;
                    }
                }
            }
        }

        opdSet.clear();
        constraint.clear();
        cons_bomb.clear();
        return ifOperated;
    }

    //以下はメソッド-------------------------------------------------------------------------------------------------------
    //爆弾集合を更新
    private void updateBombSet(){
        int x;
        int y;
        TreeSet<String> trashBox = new TreeSet<>();

        for(String i : bombSet){
            x = parse(i)[0];
            y = parse(i)[1];
            if(board.getCell(x, y) != -1){
                trashBox.add(i);
            }
        }

        for(String j : trashBox){
            bombSet.remove(j);
        }
    }

    //空き集合を更新
    private void updateEmptySet(){
        int x;
        int y;
        TreeSet<String> trashBox = new TreeSet<>();

        for(String i : emptySet){
            x = parse(i)[0];
            y = parse(i)[1];
            if(board.getCell(x, y) != -1){
                trashBox.add(i);
            }
        }

        for(String j : trashBox){
            emptySet.remove(j);
        }
    }

    //一番多くの点数が取れるマスを選ぶ
    private String seekMaxScore(TreeSet<String> s1){
        int score = 0;
        int max = 0;
        int ratio = 0;
        int x;
        int y;
        String maxGrid = "";

        for(String i : s1){
            x = parse(i)[0];
            y = parse(i)[1];
            ratio = getFlagSuccessRatio(x, y);
            score = (ratio == 0 ? 50 : calcSumOpenedCells(x, y) * ratio) ;
            if(score >= max){
                max = score;
                maxGrid = i;
            }
        }

        return maxGrid;
    }

    //周囲に-1のセルがあって、かつ自身がオープンされたセルを探す
    private TreeSet findMeetGrid(){
        TreeSet<String> result = new TreeSet<>();
        result.clear();

        for(int i = 0; i < height; i++){
            for(int j = 0; j < width; j++){
                if(1 <= board.getCell(j, i) && board.getCell(j, i) <= 8){  //opened cell
                    int[][] indices = getNeighborIndices(j, i);
                    for(int[] k : indices){
                        if(board.getCell(k[0], k[1]) == -1){
                            result.add(load(j, i));
                            break;
                        }
                    }
                }
            }
        }

        return result;
    }

    //あるマスの周りの残り爆弾数を探す
    private int remainingBomb(int x, int y){
        int[][] indices = getNeighborIndices(x, y);
        int num = 0;

        for(int[] i : indices){
            if(board.getCell(i[0], i[1]) == 10 || board.getCell(i[0], i[1]) == 9) num = num + 1;
        }
        return board.getCell(x, y) - num;
    }

    //あるマスの周りの未オープンのマス数
    private int num_unopenedCell(int x, int y){
        int[][] indices = getNeighborIndices(x, y);
        int num = 0;

        for(int[] i : indices){
            if(board.getCell(i[0], i[1]) == -1) num = num + 1;
        }
        return num;
    }    

   //あるマスの周りの爆弾数
    private int bombNum(int x, int y){
        int[][] indices = getNeighborIndices(x, y);
        int num = 0;

        for(int[] i : indices){
            if(board.getCell(i[0], i[1]) == 10 || board.getCell(i[0], i[1]) == 9) num = num + 1;
        }
        return num;
    }

    //盤面の残りの爆弾数
    private int AllRemainingBombs(){
        int num = 0;
        for(int i = 0; i < height; i++){
            for(int j = 0; j < width; j++){
                if(board.getCell(j, i) == 10 || board.getCell(j, i) == 9){
                    num = num + 1;
                }
            }
        }
        return board.getNumBombs()-num;
    }

    //盤面の未オープンの全てのマス
    private ArrayList<String> findUnopenedGrid(){
        ArrayList<String> result = new ArrayList<>();
        result.clear();

        for(int i = 0; i < height; i++){
            for(int j = 0; j < width; j++){
                if(board.getCell(j, i) == -1){
                    result.add(load(j, i));
                }
            }
        }
        return result;
    }

    //爆弾オープンの場合一番減点の少ないマスを選び出してオープンする
    private String seekMinPenalty(ArrayList<String> l){
        String minGrid = "";
        double penaltyScore = 0.0, min = -10000.0;

        for(String i : l){
            int x = parse(i)[0];
            int y = parse(i)[1];
            if(calcSumOpenedCells(x, y) == 0){
                penaltyScore = -10.0 * (AllRemainingBombs() / (height*width - board.getNumOpenedCells()));
            }
            else{
                penaltyScore = -10.0 * calcSumOpenedCells(x, y) * calcProbability(x, y);
            }
            if(penaltyScore > min){
                min = penaltyScore;
                minGrid = i;
            }
        }
        return minGrid;
    }

    //あるマスが爆弾である確率を計算
    private double calcProbability(int x, int y){
        double p = 0.0;
        double maxProbability = 0.0;
        int[][] indices = getNeighborIndices(x, y);
        
        for(int[] i : indices){
            p = (double)remainingBomb(i[0], i[1]) / num_unopenedCell(i[0], i[1]) ;
            if(p >= maxProbability){
                maxProbability = p;
            }
        }
        return maxProbability;
    }


    //周りのオープン済みのマスの和を計算する
    private int calcSumOpenedCells(int x, int y){
        int[][] indices = getNeighborIndices(x, y);
		int sum = 0;
		for (int i=0; i<indices.length; i++) {
			int nx = indices[i][0];
			int ny = indices[i][1];
			if (board.getCell(nx, ny)!=-1
					&& board.getCell(nx, ny)<9) {
					sum += board.getCell(nx, ny);
			}
		}
		return sum;
    }

    //フラグ成功の倍率を返す
    private int getFlagSuccessRatio(int x, int y) {
		int max   = seekMaxCell(x, y);
		int ratio = 0;
		if (max<=4)      { ratio =   1; }
		else if (max==5) { ratio =  10; }
		else if (max==6) { ratio =  20; }
		else if (max==7) { ratio =  50; }
		else if (max==8) { ratio = 100; }
		return ratio;
	}

    //周囲のマスを返す
    private int[][] getNeighborIndices(int x, int y){

        int indices[][];
		// 周辺のマスの数が3の場合
		if ((x==0 && y==0)
				|| (x==0 && y==height-1)
				|| (x==width-1 && y==0)
				|| (x==width-1 && y==height-1)) {
			indices = new int[3][2];
		}
		// 周辺のマスの数が5の場合
		else if (x==0
					|| x==width-1
					|| y==0
					|| y==height-1) {
			indices = new int[5][2];
		}
		// 周辺のマスの数が8の場合
		else {
			indices = new int[8][2];
		}

		int numNeighborCells = 0;
		for (int dy=-1; dy<=1; dy++) {
			if ((y==0 && dy==-1)                 // 最上行の上
					|| (y==height-1 && dy==1)) { // 最下行の下
				continue;
			}
			for (int dx=-1; dx<=1; dx++) {
				if ((x==0 && dx==-1)             // 最左列の左
					|| (x==width-1 && dx==1)) {  // 最右列の右
					continue;
				}
				if (dy==0 && dx==0) {            // 自分自身
					continue;
				}
				indices[numNeighborCells][0] = x+dx;
				indices[numNeighborCells][1] = y+dy;
				numNeighborCells++;
			}
		}
		return indices;
    }

    //周囲の最大数字を返す
    private int seekMaxCell(int x, int y){
        int max = 0;
		int[][] indices = getNeighborIndices(x, y);

		for (int i=0; i<indices.length; i++) {
			int nx = indices[i][0];
			int ny = indices[i][1];
			if (board.getCell(nx, ny)<9
					&& max<board.getCell(nx, ny)) {
					max = board.getCell(nx, ny);
			}
		}

		return max;
    }

    //マスを配列に入れるために、例えば（1，0）→“0100”に変換
    private String load(int x, int y){
        if(x < 10 && y < 10){
            return "0" + x + "0" + y;
        }
        if(10 <= x && x <= 99 && y < 10){
            return "" + x + "0" + y;
        }
        if(x < 10 && 10 <= y && y <= 99){
            return "0" + x + y;
        }
        else return "" + x + y;
    }

    //loadの逆操作　“0100”を整数に変換する
    private int[] parse(String str){
        int[] xy = new int[2];
        xy[0] = Integer.parseInt(str.substring(0, 2));   // x
        xy[1] = Integer.parseInt(str.substring(2, 4));   // y
        return xy;
    }
}