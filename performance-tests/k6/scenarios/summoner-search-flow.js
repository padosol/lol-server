import http from 'k6/http';
import { sleep, check, group } from 'k6';
import { SharedArray } from 'k6/data';
import { BASE_URL, ENDPOINTS, HEADERS, DEFAULT_REGION } from '../lib/config.js';
import {
    validateResponse,
    validateApiSuccess,
    customMetrics,
    randomItem,
    randomInt,
    generateSearchQuery,
} from '../lib/helpers.js';
import { loadThresholds } from '../thresholds.js';

/**
 * Summoner Search Flow (핵심 사용자 여정 부하 테스트)
 *
 * 목적: 핵심 사용자 여정 부하 테스트
 * 설정: 10→50 VUs 램프업, 5분
 * 플로우:
 *   1. 자동완성 검색 (5회)
 *   2. 소환사 상세 조회
 *   3. 리그 정보 조회
 *   4. 매치 목록 조회 (3페이지)
 *   5. 랭크 챔피언 통계
 */

// 테스트 데이터 로드
const testSummoners = new SharedArray('summoners', function () {
    return JSON.parse(open('../../data/test-summoners.json'));
});

// k6 옵션 - 램프업 패턴
export const options = {
    stages: [
        { duration: '30s', target: 10 },   // 워밍업: 10 VUs
        { duration: '1m', target: 30 },    // 램프업: 30 VUs
        { duration: '2m', target: 50 },    // 피크: 50 VUs
        { duration: '1m', target: 30 },    // 램프다운: 30 VUs
        { duration: '30s', target: 0 },    // 쿨다운: 0 VUs
    ],
    thresholds: loadThresholds,
    tags: {
        testType: 'load',
        scenario: 'summoner-search-flow',
    },
};

// 요청 옵션
const requestOptions = {
    headers: HEADERS,
    timeout: '30s',
};

export default function () {
    // 랜덤 소환사 선택
    const summoner = randomItem(testSummoners);
    const region = summoner.region || DEFAULT_REGION;
    const gameName = summoner.gameName;
    let puuid = summoner.puuid;

    // ========================================
    // Phase 1: 자동완성 검색 (사용자 타이핑 시뮬레이션)
    // ========================================
    group('Phase 1: Autocomplete Search', function () {
        // 사용자가 점진적으로 타이핑하는 것을 시뮬레이션
        const searchLengths = [1, 2, 3, 4, Math.min(5, gameName.length)];

        for (let i = 0; i < searchLengths.length; i++) {
            const query = generateSearchQuery(gameName, searchLengths[i]);
            const url = `${BASE_URL}${ENDPOINTS.summoner.autocomplete(query, region)}`;
            const response = http.get(url, requestOptions);

            customMetrics.autocompleteResponseTime.add(response.timings.duration);

            const isValid = validateResponse(response, `Autocomplete (${query})`);
            customMetrics.autocompleteSuccessRate.add(isValid);

            // 타이핑 딜레이 시뮬레이션 (100-300ms)
            sleep(randomInt(100, 300) / 1000);
        }
    });

    // Think time (사용자가 검색 결과를 보는 시간)
    sleep(randomInt(500, 1500) / 1000);

    // ========================================
    // Phase 2: 소환사 상세 조회
    // ========================================
    group('Phase 2: Summoner Detail', function () {
        const url = `${BASE_URL}${ENDPOINTS.summoner.getByGameName(region, gameName)}`;
        const response = http.get(url, requestOptions);

        customMetrics.summonerResponseTime.add(response.timings.duration);

        const isValid = validateResponse(response, 'Summoner Detail');
        customMetrics.summonerSuccessRate.add(isValid);

        if (isValid) {
            const apiSuccess = validateApiSuccess(response, 'Summoner Detail');
            if (apiSuccess) {
                try {
                    const body = JSON.parse(response.body);
                    puuid = body.data?.puuid || puuid;
                } catch (e) {
                    console.warn('Failed to extract puuid');
                }
            }
        }
    });

    // Think time
    sleep(randomInt(300, 800) / 1000);

    // puuid가 필요한 API들
    if (!puuid) {
        console.warn(`No puuid available for ${gameName}, skipping dependent APIs`);
        return;
    }

    // ========================================
    // Phase 3: 리그 정보 조회
    // ========================================
    group('Phase 3: League Info', function () {
        const url = `${BASE_URL}${ENDPOINTS.league.getByPuuid(puuid)}`;
        const response = http.get(url, requestOptions);

        customMetrics.leagueResponseTime.add(response.timings.duration);

        const isValid = validateResponse(response, 'League Info');
        customMetrics.leagueSuccessRate.add(isValid);

        if (isValid) {
            validateApiSuccess(response, 'League Info');
        }
    });

    // Think time
    sleep(randomInt(200, 500) / 1000);

    // ========================================
    // Phase 4: 매치 목록 조회 (3페이지 - 스크롤 시뮬레이션)
    // ========================================
    group('Phase 4: Match List (Pagination)', function () {
        const pageSize = 10;

        for (let page = 0; page < 3; page++) {
            const url = `${BASE_URL}${ENDPOINTS.match.getMatches(puuid, page, pageSize)}`;
            const response = http.get(url, requestOptions);

            customMetrics.matchResponseTime.add(response.timings.duration);

            const isValid = validateResponse(response, `Match List (page ${page})`);
            customMetrics.matchSuccessRate.add(isValid);

            if (isValid) {
                const apiSuccess = validateApiSuccess(response, `Match List (page ${page})`);

                // 매치 데이터가 없으면 페이지네이션 중단
                if (apiSuccess) {
                    try {
                        const body = JSON.parse(response.body);
                        const content = body.data?.content || [];
                        if (content.length === 0) {
                            break; // 더 이상 데이터 없음
                        }
                    } catch (e) {
                        // 파싱 실패 시 계속 진행
                    }
                }
            }

            // 스크롤 딜레이 시뮬레이션
            sleep(randomInt(500, 1000) / 1000);
        }
    });

    // Think time
    sleep(randomInt(300, 700) / 1000);

    // ========================================
    // Phase 5: 랭크 챔피언 통계
    // ========================================
    group('Phase 5: Rank Champions', function () {
        // 솔로랭크 (420)
        const soloUrl = `${BASE_URL}${ENDPOINTS.rank.champions(puuid, 420)}`;
        const soloResponse = http.get(soloUrl, requestOptions);

        customMetrics.rankChampionsResponseTime.add(soloResponse.timings.duration);

        const soloValid = validateResponse(soloResponse, 'Rank Champions (Solo)');
        customMetrics.rankChampionsSuccessRate.add(soloValid);

        if (soloValid) {
            validateApiSuccess(soloResponse, 'Rank Champions (Solo)');
        }

        sleep(randomInt(200, 400) / 1000);

        // 자유랭크 (440)
        const flexUrl = `${BASE_URL}${ENDPOINTS.rank.champions(puuid, 440)}`;
        const flexResponse = http.get(flexUrl, requestOptions);

        customMetrics.rankChampionsResponseTime.add(flexResponse.timings.duration);

        const flexValid = validateResponse(flexResponse, 'Rank Champions (Flex)');
        customMetrics.rankChampionsSuccessRate.add(flexValid);

        if (flexValid) {
            validateApiSuccess(flexResponse, 'Rank Champions (Flex)');
        }
    });

    // 세션 종료 전 Think time
    sleep(randomInt(1000, 2000) / 1000);
}

// 테스트 종료 후 요약
export function handleSummary(data) {
    const summary = {
        testType: 'Summoner Search Flow Load Test',
        timestamp: new Date().toISOString(),
        totalRequests: data.metrics.http_reqs?.values?.count || 0,
        failedRequestsRate: (data.metrics.http_req_failed?.values?.rate * 100 || 0).toFixed(2) + '%',
        avgDuration: (data.metrics.http_req_duration?.values?.avg || 0).toFixed(2) + 'ms',
        p95Duration: (data.metrics.http_req_duration?.values?.['p(95)'] || 0).toFixed(2) + 'ms',
        p99Duration: (data.metrics.http_req_duration?.values?.['p(99)'] || 0).toFixed(2) + 'ms',
        apiMetrics: {
            summoner: {
                avgDuration: (data.metrics.summoner_response_time?.values?.avg || 0).toFixed(2) + 'ms',
                successRate: ((data.metrics.summoner_success_rate?.values?.rate || 0) * 100).toFixed(2) + '%',
            },
            autocomplete: {
                avgDuration: (data.metrics.autocomplete_response_time?.values?.avg || 0).toFixed(2) + 'ms',
                successRate: ((data.metrics.autocomplete_success_rate?.values?.rate || 0) * 100).toFixed(2) + '%',
            },
            match: {
                avgDuration: (data.metrics.match_response_time?.values?.avg || 0).toFixed(2) + 'ms',
                successRate: ((data.metrics.match_success_rate?.values?.rate || 0) * 100).toFixed(2) + '%',
            },
            league: {
                avgDuration: (data.metrics.league_response_time?.values?.avg || 0).toFixed(2) + 'ms',
                successRate: ((data.metrics.league_success_rate?.values?.rate || 0) * 100).toFixed(2) + '%',
            },
            rankChampions: {
                avgDuration: (data.metrics.rank_champions_response_time?.values?.avg || 0).toFixed(2) + 'ms',
                successRate: ((data.metrics.rank_champions_success_rate?.values?.rate || 0) * 100).toFixed(2) + '%',
            },
        },
    };

    console.log('\n=== Summoner Search Flow Load Test Summary ===');
    console.log(JSON.stringify(summary, null, 2));

    return {
        'stdout': JSON.stringify(summary, null, 2),
    };
}
