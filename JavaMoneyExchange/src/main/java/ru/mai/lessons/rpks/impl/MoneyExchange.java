package ru.mai.lessons.rpks.impl;

import ru.mai.lessons.rpks.IMoneyExchange;
import ru.mai.lessons.rpks.exception.ExchangeIsImpossibleException;

import java.util.Arrays;
import java.util.Collections;

public class MoneyExchange implements IMoneyExchange {
  public String exchange(Integer sum, String coinDenomination) throws ExchangeIsImpossibleException {
    if (coinDenomination == null || coinDenomination.trim().isEmpty()) {
      throw new ExchangeIsImpossibleException("no coinDenomination provided");
    }
    Integer[] coins = parseCoinDenomination(coinDenomination);
    Arrays.sort(coins, Collections.reverseOrder());

    StringBuilder result = new StringBuilder();
    if (!coinExchangePossible(coins, sum, 0, result)) {
      throw new ExchangeIsImpossibleException("exchange is not possible");
    }
    if (result.length() > 2) {
      result.delete(result.length() - 2, result.length());
    }
    return result.toString();
  }

  public Integer[] parseCoinDenomination(String coinDenomination) {
    String[] coinStrings = coinDenomination.split(", ");
    Integer[] coins = new Integer[coinStrings.length];

    for (int i = 0; i < coinStrings.length; i++) {
      coins[i] = Integer.parseInt(coinStrings[i]);
    }
    return coins;
  }

  private boolean coinExchangePossible (Integer[] coins, int currSum, int currIndx, StringBuilder result) {
    if (currSum == 0) {
      return true;
    }
    if (currSum < 0 || currIndx > coins.length) {
      return false;
    }

    int currCoin = coins[currIndx];
    if (currCoin <= 0) {
      return false;
    }
    int maxCountThis = currSum / currCoin;

    for (int count = maxCountThis; count > 0; --count) {
      int remainder = currSum - count * currCoin;
      if (coinExchangePossible(coins, remainder, currIndx + 1, result)) {
        result.insert(0, currCoin + "[" + count + "], ");
        return true;
      }
    }
    return false;
  }
}

