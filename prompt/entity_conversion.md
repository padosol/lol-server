@module/infra/persistence/postgresql/ 하위 entity 에 해당하는 클래스들의 명칭을 *Entity 로 수정해주고, 
@Table 어노테이션을 사용해서 테이블명을 지정해주세요.

테이블 명 작성 예시는 다음과 같습니다.
- MatchEntity 일 경우 @Table(name = "match") 과 같이 매핑합니다.