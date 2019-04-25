package com.toly1994.tolymusic.app.utils;

import com.github.stuxuhai.jpinyin.PinyinHelper;
import com.toly1994.tolymusic.app.domain.Artist;
import com.toly1994.tolymusic.app.domain.Song;
import com.toly1994.tolymusic.itf.SortByLetterString;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class SongsSortByLettersUtils{
	public static int MUSIC_TOTAL= 0;
	/**
	 * 将指定集合和首字母组合成一个顺序集合</br>
	 * 形如:</br>
	 * A</br>
	 * &nbsp;item</br>
	 * &nbsp;item</br>
	 * B</br>
	 * &nbsp;item</br>
	 * &nbsp;item</br>
	 * @param list 需要排序的原始集合
	 * @param sign 提供排序依据字符串标识的接口
	 * @return 字母和内容按顺序排列好的集合
	 */
	public static ArrayList<Object> getInfoByLetter(
			List<?> list,SortByLetterString sign) {
		ArrayList<Object> sortList = new ArrayList<>();
		ArrayList<Integer> otherLetterPositons = new ArrayList<>();  //记录不属于A-Z之间的item位置
		ArrayList<String> hasLetters = new ArrayList<>();		     //记录已经收录过的字母
		Object item = null;
		String name = null; //排序依据字符串
		for (int position = 0; position < list.size(); position++) {
			// 获取实例
			item = list.get(position);
			// 获取排序依据字符串
			name = sign.sortSign(position);
			// 获取首字母
			String firstLetter = PinyinHelper.getShortPinyin(
					name.substring(0, 1)).toUpperCase(Locale.ENGLISH);
			// 处理不在A-Z之间的字符
			if (!(firstLetter.toCharArray()[0] >= 'A' && firstLetter
					.toCharArray()[0] <= 'Z')) {
				firstLetter = "#";
				otherLetterPositons.add(position);// 记录不在A-Z的位置
				continue;
			}
			// 先把首字母在A-Z之间的歌曲加入列表
			//第一次添加
			if(sortList.size()==0){
				sortList.add(firstLetter);
				sortList.add(item);
			}else{
				//没有收录过该首字母
				if(!hasLetters.contains(firstLetter)){
					//集合内的第一个元素为最小首字母
					String smllerLetter = hasLetters.get(0);
					if(firstLetter.compareTo(smllerLetter) < 0){
						//如果比最小还小,那么置顶内容
						sortList.add(0,firstLetter);
						sortList.add(1,item);
					}else{
						//新字母不是最小的字母的情况
						//遍历首字母收录集合,找到第一个大于新字母的旧字母位置
						int oldIndex = -1;
						for(int i = 0; i < hasLetters.size(); i++){
							String oldLetter = hasLetters.get(i);
							if(firstLetter.compareTo(oldLetter) < 0){
								//找到这个旧字母在原始集合内的位置
								oldIndex = sortList.indexOf(oldLetter);
								break;
							}
						}
						if(oldIndex == -1){
							//没有找到第一个大于新字母的旧字母位置,那么将内容添加到集合最后
							sortList.add(firstLetter);
							sortList.add(item);
						}else{
							sortList.add(oldIndex,firstLetter);
							sortList.add(oldIndex+1,item);
						}
					}
				}else{
					//该首字母已被收录,添加到该首字母的类别列表底
					int index = sortList.indexOf(firstLetter);
					sortList.add(index+1,item);
					continue;
				}
			}
			//记录已存在的字母
			hasLetters.add(firstLetter);
			Collections.sort(hasLetters);
		}
		// 最后把不在A-Z之间的歌曲加到集合最后
		if (otherLetterPositons.size() != 0) {
			sortList.add("#");
			for (int position = 0; position < otherLetterPositons.size(); position++) {
				sortList.add(list.get(otherLetterPositons.get(position)));
			}
			MUSIC_TOTAL = sortList.size() - hasLetters.size() + 1;
		}else{
			MUSIC_TOTAL = sortList.size() - hasLetters.size();
		}
		
		// 集合长度已经不会变化,整理集合长度
		sortList.trimToSize();
		otherLetterPositons.clear();
		hasLetters.clear();
		hasLetters = null;
		otherLetterPositons = null;
		return sortList;
	}

	/**
	 * 将指定集合和首字母组合成一个顺序集合
	 * @param list 经过首字母排序后的集合
	 * @deprecated 需要传入排序好的集合,而排序需要花费时间过久,废弃
	 */
	public static <T extends Object> ArrayList<Object> getSongsByLetter(
			ArrayList<T> list) {
		ArrayList<Object> rightList = new ArrayList<>();
		ArrayList<Integer> positons = new ArrayList<>();
		String tempLetter = null;
		Object item = null;
		String name = null;
		for (int position = 0; position < list.size(); position++) {
			// 获取Song实例
			item = list.get(position);
			// 获取歌曲名
			if (item instanceof Song) {
				Song sItem = (Song) item;
				name = sItem.getTitle();
			} else if (item instanceof Artist) {
				Artist aItem = (Artist) item;
				name = aItem.getSingerName();
			} else {
				return null;
			}
			// 获取首字母
			String firstLetter = PinyinHelper.getShortPinyin(
					name.substring(0, 1)).toUpperCase(Locale.ENGLISH);
			// 处理不在A-Z之间的字符
			if (!(firstLetter.toCharArray()[0] >= 'A' && firstLetter
					.toCharArray()[0] <= 'Z')) {
				firstLetter = "#";
				positons.add(position);// 记录不在A-Z的位置
				continue;
			}
			// 先把首字母在A-Z之间的歌曲加入列表
			if (!firstLetter.equals("#")) {
				// 如果本次获取的歌曲字母与上一次的相同,那么就不需要把实例加入集合
				if (firstLetter.equals(tempLetter)) {
					rightList.add(item);
				} else {
					rightList.add(firstLetter);
					rightList.add(item);
				}
				// 更新判断标识的字母,让下一次循环知道上一个字母是什么
				tempLetter = firstLetter;
			}
		}
		// 最后把不在A-Z之间的歌曲加到集合最后
		if (positons.size() != 0) {
			rightList.add("#");
			for (int position = 0; position < positons.size(); position++) {
				rightList.add(list.get(positons.get(position)));
			}
		}
		// 集合长度已经不会变化,整理集合长度
		rightList.trimToSize();
		positons.clear();
		return rightList;
	}
}
